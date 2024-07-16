package org.unibo.scooby
package utility.http

import java.net
import java.net.URI
import scala.annotation.targetName
import scala.math.Ordered
import scala.util.{Failure, Success, Try}

/**
 * Class that represents a URL used for HTTP calls.
 * @param protocol
 *   protocol used (e.g. "https")
 * @param host
 *   host of the URL (e.g. "www.google.com")
 * @param port
 *   port of the URL. [[Some]] containing the port or [[Empty]] if the URL doesn't contain a port.
 * @param path
 *   path of the URL (e.g. "/example/path")
 * @param queryParams
 *   [[Map]] that contains the query params of the URL (e.g. "service=mail&passive=true")
 * @param fragment
 *   fragment of the URL. [[Some]] containing the fragment (e.g. "#section") or [[None]] if there is no fragment
 */
enum URL private (
  val protocol: String,
  private val host: String,
  private val port: Option[Int],
  val path: String,
  val queryParams: Map[String, String],
  val fragment: Option[String]
) extends Ordered[URL]:

  case Absolute(override val protocol: String,
                private val host: String,
                private val port: Option[Int],
                override val path: String,
                override val queryParams: Map[String, String],
                override val fragment: Option[String]) extends URL(protocol, host, port, path, queryParams, fragment)

  case Relative(override val path: String, override val fragment: Option[String]) 
    extends URL("", "", Option.empty, path, Map.empty, fragment)

  case Invalid() extends URL("", "", Option.empty, "", Map.empty, Option.empty)


  extension(path: String)
    private def withoutLeadingSlashes: String = path.replaceAll("^/+", "")
    private def withoutTrailingSlashes: String = path.replaceAll("/+$", "")

    @targetName("appendPath")
    infix def /(otherPath: String): String = path.withoutTrailingSlashes + "/" + otherPath.withoutLeadingSlashes


  /**
   * Gets the URL without the protocol (e.g. "http://www.example.com/example" => "www.example.com/example")
   * @return the URL string without the protocol
   */
  def withoutProtocol: String = domain + path + queryString + fragmentString

  /**
   * Gets the domain from the URL (e.g. "http://www.example.com/example" => "www.example.com")
   * @return
   *   a [[String]] containing the domain
   */
  def domain: String = host + portString

  /**
   * Gets the "parent" of this URL, that is the preceding path (e.g. the parent of "/example/path" is "/example/")
   * @return
   *   the parent [[URL]] of this URL.
   */
  def parent: URL =
    def dropLast(path: String) = path.split("/").dropRight(1).mkString("/") + "/"

    this match
    case Absolute(protocol, host, port, path, queryParams, fragment) => Absolute(protocol, host, port,
      dropLast(path), Map.empty, Option.empty)
    case Relative(path, fragment) => Relative(dropLast(path), Option.empty)
    case Invalid() => Invalid()

  /**
   * Gets the "depth" of this URL's path. For example a URL `https://www.example.com` has depth 0 and
   * `https://www.example.com/example/1` has depth 2
   * @return
   *   the depth of this URL
   */
  def depth: Int = if path.length > 1 then path.split("/").length else 0

  /**
   * Compares the depth of two URLs. Returns a value greater that 0 if this URL is has greater depth, equal to 0 if they
   * have the same depth, less than 0 otherwise
   * @param that
   *   [[URL]] to compare with this URL
   * @return
   *   a [[Int]] value representing the result of the comparison
   */
  override def compare(that: URL): Int =
    depth compare that.depth

  /**
   * Utility method that appends to this URL a [[String]], placing the trailing backslashes correctly
   * @param other
   *   the path (e.g. "/example") to be appended to this URL
   * @return
   *   a new [[URL]] with the new path appended
   */
  @targetName("append")
  infix def /(other: String): URL = this / URL.Absolute(protocol, host, port, other, queryParams, fragment)



  /**
   * Utility method that appends an another [[URL]] to this URL, with the same protocol, host and post, placing the
   * trailing backslashes correctly
   * @param other
   *   URL to be appended
   * @return
   *   a new [[URL]] with the new path appended
   */
  @targetName("append")
  infix def /(other: URL): URL =
    this match
      case URL.Absolute(protocol, host, port, path, queryParams, fragment) => Absolute(
        protocol,
        host,
        port,
        path.withoutTrailingSlashes + "/" + other.path.withoutLeadingSlashes,
        Map.empty,
        Option.empty
      )
      case URL.Relative(path, fragment) => Relative(path.withoutTrailingSlashes + "/" + other.withoutLeadingSlashes, 
        fragment)
      case URL.Invalid() => this
      
  private def resolveRelative(root: URL, relative: URL): URL =
    import utility.http.URL.toAbsolute
    relative.path match
    case x if x.isEmpty => Invalid()
    case _ =>
      Try:
        new net.URI(root.toString).resolve(new URI(relative.toString)).toURL.toAbsolute
      .getOrElse(Invalid())


  def resolve(root: URL): URL =
    this match
    case x: Absolute => this
    case x: Relative => resolveRelative(root, x)
    case x: Invalid => x
    
  def isValid: Boolean = this match
    case Invalid() => false
    case _ => true
    
  def isRelative: Boolean = this match
    case _ : Relative => true
    case _ => false

  def isAbsolute: Boolean = this match
    case _ : Absolute => true
    case _ => false

  /**
   * Gets the port string (e.g. if a URL has port 4000, it returns ":4000"). 
   *
   * @return the port string <b>or</b> empty string if the URL has no port
   */
  def portString: String = port.map(":" + _).getOrElse("")

  /**
   * Gets the query string (e.g. "https://www.example.com?fo=1&ol=2" => "?fo=1&ol=2")
   * @return the query string <b>or</b> empty string if the URL has no query parameter
   */
  def queryString: String = if (queryParams.isEmpty) ""
    else "?" + queryParams.map { case (k, v) => s"$k=$v" }.mkString("&")

  /**
   * Gets the fragment string (e.g. "https://www.example.com#fragment" => "#fragment")
   * @return the fragment string <b>or</b> empty string if the URL has no fragment
   */
  def fragmentString: String = fragment.map("#" + _).getOrElse("")

  override def toString: String =
    val protocolPrefix = if protocol.nonEmpty then s"$protocol://" else ""
    s"$protocolPrefix$host$portString$path$queryString$fragmentString"

object URL:

  private def parseQueryParams(queryString: Option[String]): Map[String, String] =
    queryString.fold(Map.empty[String, String]) {
      _.drop(1)
        .split("&")
        .filter(_.contains("="))
        .map(_.split("="))
        .filter(_.length == 2)
        .collect:
          case Array(key, value) =>
            key -> value
        .toMap
    }

  extension (x: java.net.URL)
    private def toAbsolute: URL =
      Absolute(x.getProtocol, x.getHost, x.getPort match { case -1 => None; case port => Some(port) },
        x.getPath, parseQueryParams(Option(x.getQuery)), Option(x.getRef))

  extension (x: java.net.URI)
    private def toRelative: URL =
      Relative(x.getPath, Option(x.getFragment))
  /**
   * Entry point for instantiating a URL. It parses the provided [[String]].
   * @param url
   *   String parsed as URL
   * @return
   *   a [[Either]] with a String representing an error or a [[URL]] if the parsing was successful
   */
  def apply(url: String): URL =

    val delegateTry = Try(new java.net.URI(url))
    delegateTry match
      case Failure(_) => Invalid()
      case Success(delegate) =>
        delegate match
          case x if x.isAbsolute =>
            Try:
              x.toURL.toAbsolute
            .getOrElse(Invalid())
          case x =>
            x.toRelative


  extension(string: String)
    /**
     * Extension method that converts a [[String]] to a [[URL]]
     * @return
     *   a [[Either]] with a String representing an error or a [[URL]] if the parsing was successful
     */
    def toUrl: URL = URL(string)

  extension(string: StringContext)
    /**
     * Extension method that converts a [[StringContext]] to a [[URL]]
     * @return
     *   a [[Either]] with a String representing an error or a [[URL]] if the parsing was successful
     */
    def url(args: Any*): URL = URL(string.s(args*))

  /**
   * Used to generate an empty URL, mainly as placeholder or for testing purposes.
   * @return
   *   an empty URL
   */
  def empty: URL = Invalid()
