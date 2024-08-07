package org.unibo.scooby
package utility.http

/**
 * Method selected for the HTTP request
 */
enum HttpMethod:
  case GET
  case POST
  case PUT
  case DELETE

object HttpMethod:
  /**
   * Utility method to get the HTTP method from its name (not case sensitive)
   * @param name
   *   name of the method
   * @return
   *   a [[HttpMethod]] corresponding to the provided name
   */
  def of(name: String): HttpMethod = HttpMethod.valueOf(name.toUpperCase())

/**
 * Status reported inside the HTTP response. A special value "INVALID" is provided to mark a response as invalid (a.k.a.
 * something failed)
 * @param code
 *   status code of the response
 * @param description
 *   textual description of the status
 */
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
  /**
   * Utility method to get the HTTP status from its code
   * @param code
   *   code of the HTTP response status
   * @return
   *   an [[Option]] containing the HTTP status if the provided code matches an existing status code, or an [[None]]
   *   otherwise
   */
  def of(code: Int): Option[HttpStatus] = HttpStatus.values.find(_.code == code)

/**
 * Type representing a group of headers of a Request or Response
 */
type Headers = Map[String, String]

/**
 * Type representing a single header of a Request or Response
 */
type Header = (String, String)

/**
 * Body of a Request/Response
 */
type Body = String

/**
 * Class used to wrap an HTTP request.
 * @param method
 *   HTTP method for this request
 * @param url
 *   destination URL
 * @param headers
 *   headers provided for the request
 * @param body
 *   body of the request, can be [[None]]
 */
sealed case class Request private (
  method: HttpMethod,
  url: URL,
  headers: Headers,
  body: Option[Body]
):
  /**
   * Sends the built HTTP request.
   *
   * @param client
   *   client through which this request is sent. The provided client must mix-in a [[Backend]] that works with
   *   responses of type [[R]]
   * @tparam R
   *   type of responses that the client's backend works with
   * @return
   *   a [[Right]] of [[T]] if the request went good (no network exceptions), [[Left]] of [[HttpError]] representing an
   *   error otherwise.
   */
  def send[R](client: Client[R]): Either[HttpError, R] =
    if client.requestCount >= client.configuration.maxRequests then
      Left("Reached the maximum amount of requests".asHttpError)
    else
      try Right(client.sendAndIncrement(this))
      catch
        case ex: Exception => Left(ex.asHttpError)

/**
 * Class used to wrap an HTTP response
 * @param status
 *   status of the response
 * @param headers
 *   headers provided by the HTTP response
 * @param body
 *   body of the response, can be [[None]]
 * @param request
 *   request to which this Response is answering to
 */
sealed case class Response(
  status: HttpStatus,
  headers: Headers,
  body: Option[Body],
  request: Request
)

object Request:
  import HttpMethod.*

  /**
   * Utility class to easily build new HTTP requests
   * @param method
   *   HTTP method for this request
   * @param url
   *   destination URL
   * @param headers
   *   headers provided for the request
   * @param body
   *   body of the request, can be [[None]]
   */
  case class RequestBuilder(
    private val method: HttpMethod,
    private val url: URL,
    private val headers: Headers,
    private val body: Option[Body]
  ):
    /**
     * Defines the method GET for this request
     * @return
     *   a new [[RequestBuilder]] with GET method set
     */
    def get(): RequestBuilder = copy(method = GET)

    /**
     * Defines the method POST for this request
     * @return
     *   a new [[RequestBuilder]] with POST method set
     */
    def post(): RequestBuilder = copy(method = POST)

    /**
     * Defines the method PUT for this request
     * @return
     *   a new [[RequestBuilder]] with PUT method set
     */
    def put(): RequestBuilder = copy(method = PUT)

    /**
     * Defines the method DELETE for this request
     * @return
     *   a new [[RequestBuilder]] with DELETE method set
     */
    def delete(): RequestBuilder = copy(method = DELETE)

    /**
     * Sets the method for the built request
     * @param httpMethod
     *   [[HttpMethod]] set for this request
     * @return
     *   a new [[RequestBuilder]] with this HTTP method set
     */
    def method(httpMethod: HttpMethod): RequestBuilder = copy(method = httpMethod)

    /**
     * Sets the destination URL for the built request
     * @param url
     *   [[URL]] set for this request
     * @return
     *   a new [[RequestBuilder]] with this URL set
     */
    def at(url: URL): RequestBuilder = copy(url = url)

    /**
     * Sets the destination URL for the built request
     * @param url
     *   [[String]] converted to the [[URL]] set for this request
     * @return
     *   a new [[RequestBuilder]] with this URL set
     */
    def at(url: String): RequestBuilder = copy(url = URL(url))

    /**
     * Sets the headers for the built request
     * @param headers
     *   headers set for the built request
     * @return
     *   a new [[RequestBuilder]] with this headers set
     */
    def headers(headers: Header*): RequestBuilder = copy(headers = this.headers ++ headers)

    /**
     * Sets the body for the built request
     * @param body
     *   [[Body]] to be set for this request
     * @return
     *   a new [[RequestBuilder]] with this body set
     */
    def body(body: Body): RequestBuilder = copy(body = Some(body))

    /**
     * Builds the [[Request]]
     * @return
     *   a [[Right]] of [[Request]] if the provided URL was provided and well formatted, [[Left]] of a [[HttpError]]
     *   representing an error otherwise (es. "You must provide a valid URL")
     */
    def build: Either[HttpError, Request] = url match
      case URL.Absolute(protocol, host, port, path, queryParams, fragment) => Right(Request(method, url, headers, body))
      case URL.Relative(path, fragment) =>
        Left("Relative URL was not transformed into Absolute before making the call".asHttpError)
      case URL.Invalid() =>
        Left("Invalid URL was provided".asHttpError)


  /**
   * Used to instantiate a builder that builds [[Request]] s
   * @return
   *   a new [[RequestBuilder]] with default starting parameters
   */
  def builder: RequestBuilder = RequestBuilder(GET, URL.empty, Map.empty, Option.empty)

  /**
   * Used to instantiate an empty [[Request]], only for debugging/testing purposes
   * @return
   *   an empty [[Request]]
   */
  def empty: Request = Request(HttpMethod.GET, URL.empty, Map.empty, Option.empty)

object Response:
  /**
   * Utility instantiation method to generate an empty [[Response]]: used mainly as placeholder or for testing purposes
   * @return
   */
  def empty: Response = Response(HttpStatus.INVALID, Map.empty, Option.empty, Request.empty)
