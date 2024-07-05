package org.unibo.scooby
package utility.http.backends

import utility.http.*
import utility.http.Configuration.Property.NetworkTimeout
import utility.http.HttpStatus.INVALID

import sttp.model.RequestMetadata

import scala.concurrent.duration.FiniteDuration

/**
 * Simple, synchronous HTTP client backend that utilizes the sttp library under the hood
 */
trait SttpSyncBackend extends Backend[Response]:
  import HttpMethod.*
  import sttp.client3
  import sttp.client3.{HttpClientSyncBackend, SttpBackendOptions, UriContext, asString, basicRequest}

  import scala.concurrent.duration.DurationInt

  private type SttpRequest = client3.Request[?, Any]
  private type SttpResponse = client3.Response[?]
  private type SttpURI = sttp.model.Uri

  extension (url: URL) private def asSttpURI: SttpURI = uri"${url.toString}"

  extension (originalRequest: Request)
    private def asSttpRequest: SttpRequest =
      val request = originalRequest.method match
        case GET => basicRequest.get(originalRequest.url.asSttpURI)
        case POST => basicRequest.post(originalRequest.url.asSttpURI)
        case PUT => basicRequest.put(originalRequest.url.asSttpURI)
        case DELETE => basicRequest.delete(originalRequest.url.asSttpURI)
      request.headers(originalRequest.headers).body(originalRequest.body.getOrElse("")).response(asString)

  extension (sttpRequest: RequestMetadata)
    private def asRequest: Request =
      Request.builder
        .at(sttpRequest.uri.toString)
        .method(HttpMethod.of(sttpRequest.method.toString))
        .headers(sttpRequest.headers.map(header => (header.name, header.value))*)
        .build.getOrElse(Request.empty)

  extension (response: SttpResponse)
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

  private lazy val defaultTimeout = 5.seconds
  private lazy val actualBackend = HttpClientSyncBackend(
    options = SttpBackendOptions.connectionTimeout(configuration.property[NetworkTimeout, FiniteDuration]
      .getOrElse(defaultTimeout))
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
