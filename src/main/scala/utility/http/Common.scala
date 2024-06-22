package org.unibo.scooby
package utility.http

import scala.util.Try

enum HttpMethod:
  case GET
  case POST
  case PUT
  case DELETE

enum HttpStatus(val code: Int, val description: String):
  case INVALID extends HttpStatus(-1, "Invalid Response")
  case OK extends HttpStatus(200, "OK")
  case BAD_REQUEST extends HttpStatus(400, "Bad Request")
  case UNAUTHORIZED extends HttpStatus(401, "Unauthorized")
  case NOT_FOUND extends HttpStatus(404, "Not Found")
  case REQUEST_TIMEOUT extends HttpStatus(408, "Request Timeout")
  case TOO_MANY_REQUESTS extends HttpStatus(429, "Too many requests")
  case INTERNAL_SERVER_ERROR extends HttpStatus(500, "Internal Server Error")
  case BAD_GATEWAY extends HttpStatus(502, "Bad Gateway")
  case GATEWAY_TIMEOUT extends HttpStatus(504, "Gateway Timeout")
  
object HttpStatus:
  def of(code: Int): Option[HttpStatus] = HttpStatus.values.find(_.code == code)


type Headers = Map[String, String]
type Header = (String, String)
type Body = String


sealed case class Request private(
                                   method: HttpMethod,
                                   url: URL,
                                   headers: Headers,
                                   body: Option[Body])

sealed case class Response (
                             status: HttpStatus,
                             headers: Headers,
                             body: Option[Body]
                           )


object Request:
  import HttpMethod.*
  case class RequestBuilder(
                             private val method: HttpMethod,
                             private val url: URL,
                             private val headers: Headers,
                             private val body: Option[Body]):

    def get(): RequestBuilder = copy(method = GET)
    def post(): RequestBuilder = copy(method = POST)
    def put(): RequestBuilder = copy(method = PUT)
    def delete(): RequestBuilder = copy(method = DELETE)
    def method(httpMethod: HttpMethod): RequestBuilder = copy(method = httpMethod)
    def at(url: URL): RequestBuilder = copy(url = url)
    def at(url: String): RequestBuilder = copy(url = URL(url).getOrElse(URL.empty))
    def headers(headers: Header*): RequestBuilder = copy(headers = this.headers ++ headers)
    def body(body: Body): RequestBuilder = copy(body = Some(body))

    def send[R, T](using deserializer: Deserializer[R, T])(using client: HttpClient with Backend[R]): Try[T] =
      Try:
        deserializer.deserialize(client.send(build.get))

    def build: Try[Request] =
      Try {
        if url != URL.empty then Request(method, url, headers, body) else
          throw new IllegalArgumentException("You must provide a URL")
      }

  def builder: RequestBuilder = RequestBuilder(GET, URL.empty, Map.empty, Option.empty)

object Response:
  def empty: Response = Response(HttpStatus.INVALID, Map.empty, Option.empty)



