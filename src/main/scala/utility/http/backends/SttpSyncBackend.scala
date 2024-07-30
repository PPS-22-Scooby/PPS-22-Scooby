package org.unibo.scooby
package utility.http.backends

import org.unibo.scooby.utility.http.*
import org.unibo.scooby.utility.http.HttpStatus.INVALID
import sttp.model.RequestMetadata

/**
 * Simple, synchronous HTTP client backend that utilizes the sttp library under the hood
 */
trait SttpSyncBackend extends Backend[Response]:
  import HttpMethod.*
  import sttp.client3
  import sttp.client3.{HttpClientSyncBackend, SttpBackendOptions, UriContext, asString, basicRequest}

  /**
   * Type alias for Request of the sttp library
   */
  private type SttpRequest = client3.Request[?, Any]

  /**
   * Type alias for Response of the sttp library
   */
  private type SttpResponse = client3.Response[?]

  /**
   * Type alias for Uri of the sttp library
   */
  private type SttpURI = sttp.model.Uri

  /**
   * Simple extension to convert a [[URL]] to a [[SttpURI]]
   */
  extension (url: URL) private def asSttpURI: SttpURI = uri"${url.toString}"

  /**
   * Simple extension to convert a [[Request]] of this library to a [[SttpRequest]]
   */
  extension (originalRequest: Request)
    /**
     * Conversion utility from [[Request]] to [[SttpRequest]]
     * @return
     *   a [[SttpRequest]]
     */
    private def asSttpRequest: SttpRequest =
      val request = originalRequest.method match
        case GET => basicRequest.get(originalRequest.url.asSttpURI)
        case POST => basicRequest.post(originalRequest.url.asSttpURI)
        case PUT => basicRequest.put(originalRequest.url.asSttpURI)
        case DELETE => basicRequest.delete(originalRequest.url.asSttpURI)
      request
        .headers(originalRequest.headers)
        .headers(configuration.headers)
        .body(originalRequest.body.getOrElse("")).response(asString)

  /**
   * Simple extension to convert a [[RequestMetadata]] (obtained from inside the sttp Response) to a [[Request]].
   */
  extension (sttpRequest: RequestMetadata)
    /**
     * Conversion utility from [[RequestMetadata]] (obtained from inside the sttp Response) to a [[Request]].
     * @return
     *   a [[Request]]. <b>Note</b>: the body is empty because sttp [[RequestMetadata]] contained inside the Response
     *   doesn't maintain the original [[Request]] 's body
     */
    private def asRequest: Request =
      Request.builder
        .at(sttpRequest.uri.toString)
        .method(HttpMethod.of(sttpRequest.method.toString))
        .headers(sttpRequest.headers.map(header => (header.name, header.value))*)
        .build.getOrElse(Request.empty)

  /**
   * Simple extension to convert a [[SttpResponse]] to a [[Response]].
   */
  extension (response: SttpResponse)
    /**
     * Conversion utility from [[SttpResponse]] to a [[Response]]
     * @return
     *   a [[Response]]
     */
    private def asResponse: Response =
      Response(
        HttpStatus.of(response.code.code).getOrElse(INVALID),
        response.headers.map(header => (header.name, header.value)).toMap,
        response.body match
          case Right(x: String) => Some(x)
          case Left(x: String) => Some(x)
          case _ => None
        ,
        response.request.asRequest
      )
  /**
   * STTP backend used internally to make HTTP calls.
   */
  private lazy val actualBackend = HttpClientSyncBackend(
    options = SttpBackendOptions.connectionTimeout(configuration.networkTimeout)
  )

  /**
   * Sends the [[Request]] synchronously and blocks until a [[Response]] is generated.
   * @param request
   *   request to be sent
   * @return
   *   a response of type [[R]], depending on the Backend implementation
   */
  override def send(request: Request): Response =
    request.asSttpRequest.send(actualBackend).asResponse
