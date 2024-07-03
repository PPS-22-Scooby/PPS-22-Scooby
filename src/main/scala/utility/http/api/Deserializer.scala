package org.unibo.scooby
package utility.http

/**
 * The deserializer is a trait that transforms HTTP responses of class [[R]] in objects of class [[T]]. It can be seen
 * as a utility that makes the user avoid having to access the HTTP response and lets him/her use an already
 * deserialized object instead
 *
 * @tparam R
 *   "form" of the responses used by the backend in hand
 * @tparam T
 *   type of the object to which the responses [[R]] are deserialized
 */
trait Deserializer[R, T]:
  /**
   * Deserializes the response into a [[T]] object
   *
   * @param response
   *   response to be deserialized
   * @return
   *   a deserialized object of class [[T]]
   */
  def deserialize(response: R): T

/**
 * Collection of useful deserializers
 */
object Deserializer:
  /**
   * Default deserializer, used when the user wants directly the a [[Response]] object (in practice it's as skipping the
   * deserializing phase).
   * @return
   *   a non-deserialized [[Response]] object
   */
  given default: Deserializer[Response, Response] = (response: Response) => response

  /**
   * Utility deserializer that gives [[String]] s from [[Response]] s. It simply extracts the response's body
   * @return
   *   the [[String]] body of the Response
   */
  given body: Deserializer[Response, String] = (response: Response) => response.body.getOrElse("")

  /**
   * Utility deserializer that gives [[Option]] of [[String]] from [[Response]]. It simply extracts the response's body (if
   * present)
   * @return
   *   [[Some]] containing the body if present, [[None]] otherwise
   */
  given optionalBody: Deserializer[Response, Option[String]] = (response: Response) => response.body
