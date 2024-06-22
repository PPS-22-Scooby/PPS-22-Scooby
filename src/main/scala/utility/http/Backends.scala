package org.unibo.scooby
package utility.http

import utility.http.HttpStatus.INVALID


trait Backend[R] extends HttpClient:
  def send(request: Request): R

object Backends:
  trait SttpBackend extends Backend[Response]:
    import HttpMethod.*
    import sttp.client3
    import sttp.client3.{HttpClientSyncBackend, RequestT, UriContext, basicRequest, SimpleHttpClient as SttpClient}
    import sttp.client3.SttpBackendOptions
    import scala.concurrent.duration.DurationInt

    private type SttpRequest = client3.Request[_, Any]
    private type SttpResponse = client3.Response[_]
    private type SttpURI = sttp.model.Uri

    extension(url: URL)
      private def asSttpURI: SttpURI = uri"${url.toString}"

    extension(originalRequest: Request)
      private def asSttpRequest: SttpRequest =
        val request = originalRequest.method match
          case GET => basicRequest.get(originalRequest.url.asSttpURI)
          case POST => basicRequest.post(originalRequest.url.asSttpURI)
          case PUT => basicRequest.put(originalRequest.url.asSttpURI)
          case DELETE => basicRequest.delete(originalRequest.url.asSttpURI)
        originalRequest.headers.foreach(header => request.header(header._1, header._2))
        request.body(originalRequest.body.getOrElse(""))

    extension (response: SttpResponse)
      private def asResponse: Response = Response(
        HttpStatus.of(response.code.code).getOrElse(INVALID),
        response.headers.map(header => (header.name, header.value)).toMap,
        if response.body.toString.nonEmpty then Some(response.body.toString) else Option.empty[Body])

    private lazy val actualBackend = HttpClientSyncBackend(options = SttpBackendOptions.connectionTimeout(5.seconds))

    override def send(request: Request): Response =
      request.asSttpRequest.send(actualBackend).asResponse

