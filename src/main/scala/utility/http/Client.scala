package org.unibo.scooby
package utility.http

import utility.http.Configuration.ClientConfiguration
import utility.http.Configuration.Property.NetworkTimeout

import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

/**
 * Core of the HTTP logic. A [[HttpClient]] can mix-in a Backend to gain the corresponding, underlying implementation of
 * HTTP calls. Each Backend must specify the type of the generated HTTP responses: it can also (and in fact should) be a
 * type that does not depend on the underlying HTTP library.
 *
 * Note that while the [[Request]] type is used inside all the backends, the response's one [[R]] is specific to the
 * used Backend.
 *
 * @tparam R
 *   type of the HTTP responses generated by the Backend
 */
trait Backend[R] extends HttpClient:
  /**
   * Sends the provided [[Request]] using the Backend implementation
   *
   * @param request
   *   request to be sent
   * @return
   *   a response of type [[R]], depending on the Backend implementation
   */
  def send(request: Request): R

/**
 * Empty trait that represents an HTTP client. To be useful, you need to mix-in [[Backend]] Traits
 */
trait HttpClient(val configuration: ClientConfiguration)

/**
 * Collection of useful clients types, using different backends.
 */
object Clients:
  import utility.http.backends.SttpSyncBackend

  /**
   * Simple HTTP client used for synchronous HTTP calls with the sttp library as backend
   */
  class SimpleHttpClient(configuration: ClientConfiguration = Configuration.default)
      extends HttpClient(configuration) with SttpSyncBackend

/**
 * Type alias to represent a [[Client]] with a [[Backend]]
 * @tparam R type of the [[Backend]]
 */
type Client[R] = HttpClient & Backend[R]

/**
 * Object that contains configuration classes for [[Client]]
 */
object Configuration:
  import scala.concurrent.duration.DurationInt

  /**
   * Enum that represents various types of settings applicable to [[ClientConfiguration]]
   * @param value value of this setting
   * @tparam T type of the value of this setting
   */
  enum Property[T](val value: T):
    /**
     * Type of property that represents the network timeout. Works by setting a [[FiniteDuration]]
     */
    case NetworkTimeout(override val value: FiniteDuration) extends Property[FiniteDuration](value)
    /**
     * Type of property that represents the maximum amount of requests that can be made within this client
     */
    case MaxRequests(override val value: Int) extends Property[Int](value)

  /**
   * Class representing the configuration of a [[Client]]. It can contain various types of [[Property]].
   * @param properties [[Seq]] of [[Property]] that globally represent the collection of settings for this [[Client]].
   *                   One type of [[Property]] is meant to be added only once inside this collection.
   */
  case class ClientConfiguration(properties: Seq[Property[?]]):
    /**
     * Returns the value of the [[Property]] requested.
     * @param tag [[ClassTag]] to prevent type erasure for type [[T]], representing the [[Property]] requested
     * @tparam T type of the [[Property]] requested
     * @tparam R type of the value inside the [[Property]] requested
     * @return the value of type [[R]] inside the [[Property]]
     */
    def property[T <: Property[R], R](implicit tag: ClassTag[T]): Option[R] =
      properties.collectFirst {
        case p if tag.runtimeClass.isInstance(p) =>
          p.asInstanceOf[T].value
      }

  /**
   * Companion object for the [[ClientConfiguration]] that contains its `apply` method
   */
  object ClientConfiguration
  /**
   * Configuration used by default by the [[Client]]s
   * @return the default [[ClientConfiguration]]
   */
  def default: ClientConfiguration = ClientConfiguration(Seq(NetworkTimeout(5.seconds)))
