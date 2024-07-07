package org.unibo.scooby
package utility.http.api

import utility.http.{Header, HttpError, URL}
import utility.http.HttpMethod.{GET, POST}
import utility.http.Request.RequestBuilder

/**
 * Facade utility object to make HTTP calls faster and in a more readable way (DSL-like)
 * [[ApiTest]] inside [[org.unibo.scooby.utility.http]] contains an example usage of this API.
 */
object Calls:
  import utility.http.{Client, Deserializer, HttpMethod, Request}

  /**
   * Context used for DSL-like interaction with a Request Builder
   */
  private type RequestContext = RequestBuilderMutable ?=> Unit

  /**
   * Returns the builder of the context without touching it (builds a request with default configuration)
   * @param x$1
   *   Request builder of the context
   * @return
   *   the builder of the current context
   */
  private def buildUntouched(using RequestBuilderMutable): RequestBuilder = summon[RequestBuilderMutable].builder

  /**
   * Builds a HTTP GET request using a DSL-like fashion
   * @param url
   *   String version of the URL of the request
   * @tparam T
   *   type of the deserialized response (default is Response)
   * @tparam R
   *   type of the used Client. Must be a given Client in the scope.
   * @return
   *   a [[PartialCall]] representing the request to be sent.
   */
  def GET[T, R: Client](url: String): PartialCall[T, R] =
    new PartialCall[T, R](URL(url).getOrElse(URL.empty), HttpMethod.GET)

  /**
   * Builds a HTTP GET request using a DSL-like fashion
   * @param url
   * [[URL]] of the request
   * @tparam T
   * type of the deserialized response (default is Response)
   * @tparam R
   * type of the used Client. Must be a given Client in the scope.
   * @return
   * a [[PartialCall]] representing the request to be sent.
   */
  def GET[T, R: Client](url: URL): PartialCall[T, R] =
    new PartialCall[T, R](url, HttpMethod.GET)

  /**
   * Builds a HTTP POST request using a DSL-like fashion
   * @param url
   *   [[URL]] of the request
   * @tparam T
   *   type of the deserialized response (default is Response)
   * @tparam R
   *   type of the used Client. Must be a given Client in the scope.
   * @return
   *   a [[PartialCall]] representing the request to be sent.
   */
  def POST[T, R: Client](url: URL): PartialCall[T, R] =
    new PartialCall[T, R](url, HttpMethod.POST)

  /**
   * Builds a HTTP POST request using a DSL-like fashion
   * @param url
   * String version of the URL of the request
   * @tparam T
   * type of the deserialized response (default is Response)
   * @tparam R
   * type of the used Client. Must be a given Client in the scope.
   * @return
   * a [[PartialCall]] representing the request to be sent.
   */
  def POST[T, R: Client](url: String): PartialCall[T, R] =
    new PartialCall[T, R](URL(url).getOrElse(URL.empty), HttpMethod.POST)


  /**
   * Sets the body setting for the [[RequestBuilderMutable]] in the scope.
   * @param init
   *   a by-name [[String]] to be treated as body
   * @param mutableBuilder
   *   a [[RequestBuilderMutable]] in the scope
   */
  def Body(init: => String)(using mutableBuilder: RequestBuilderMutable): Unit =
    val mutableBuilder = summon[RequestBuilderMutable]
    mutableBuilder.builder = mutableBuilder.builder.body(init)

  /**
   * Sets the headers setting for the [[RequestBuilderMutable]] in the scope.
   * @param init
   *   a by-name [[Seq]] of [[Header]] representing the headers in form of key-value pairs
   * @param mutableBuilder
   *   a [[RequestBuilderMutable]] in the scope
   */
  def Headers(init: => Seq[Header])(using mutableBuilder: RequestBuilderMutable): Unit =
    mutableBuilder.builder = mutableBuilder.builder.headers(init*)

  /**
   * Class representing a call to be sent. The [[Conversion]] [[call2send]] provides a seamlessly way to send a partial
   * call in a hidden way.
   * @param url
   *   [[URL]] of the request
   * @param method
   *   method of the request
   * @tparam T
   *   type of the deserialized response
   * @tparam R
   *   type of the response used by the [[Client]] in the Scope
   */
  class PartialCall[T, R: Client](val url: URL, val method: HttpMethod):
    infix def sending(init: RequestContext)(using
      deserializer: Deserializer[R, T]
    ): Either[HttpError, T] =
      given mutableBuilder: RequestBuilderMutable = RequestBuilderMutable(Request.builder.at(url).method(method))
      init
      mutableBuilder.builder.build.flatMap {
        _.send[R](summon[Client[R]]).flatMap(deserializer.deserialize)
      }

  /**
   * Conversion that sends a [[PartialCall]] in a hidden way, maintaining the DSL look of the other utility methods.
   */
  given call2send[T, R: Client](using Deserializer[R, T]): Conversion[PartialCall[T, R], Either[HttpError, T]] with
    override def apply(partialCall: PartialCall[T, R]): Either[HttpError, T] =
      partialCall sending buildUntouched

  /**
   * Utility case class: essentially a container for the actual [[RequestBuilder]] that gets modified inside the DSL
   * block
   * @param builder actual immutable [[RequestBuilder]] that gets changed by this DSL instructions
   */
  sealed case class RequestBuilderMutable(var builder: RequestBuilder)
