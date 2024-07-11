package org.unibo.scooby
package utility.http

import scala.annotation.targetName
import scala.math.Ordered
import scala.util.matching.Regex

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
final case class URL private (
  protocol: String,
  private val host: String,
  private val port: Option[Int],
  path: String,
  queryParams: Map[String, String],
  fragment: Option[String]
) extends Ordered[URL]:

  /**
   * Gets the URL without the protocol (e.g. "http://www.example.com/example" => "www.example.com/example")
   * @return the URL string without the protocol
   */
  def withoutProtocol: String = domain + path + queryString + fragmentString

  /**
   * Gets the domain from the URL
   * @return
   *   a [[String]] containing the domain
   */
  def domain: String = host + portString

  /**
   * Gets the "parent" of this URL, that is the preceding path (e.g. the parent of "/example/path" is "/example/")
   * @return
   *   the parent [[URL]] of this URL.
   */
  def parent: URL = URL(protocol, host, port, path.split("/").dropRight(1).mkString("/") + "/", Map.empty, Option.empty)

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
  infix def /(other: String): URL =
    this / URL(protocol, host, port, path + other, Map.empty, Option.empty)

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
    def removeLeadingTrailingSlashes(input: String): String =
      input.replaceAll("^/+", "").replaceAll("/+$", "")

    if other.protocol == protocol && other.host == host && other.port == port then
      // warning: not giving info about malformed URLs -> just returning itself if fails
      URL.apply(URL(
        protocol,
        host,
        port,
        removeLeadingTrailingSlashes(path) + "/" + removeLeadingTrailingSlashes(other.path),
        Map.empty,
        Option.empty
      ).toString).getOrElse(this)
    else
      this

  /**
   * Gets the port string (e.g. if a URL has port 4000, it returns ":4000"). 
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
    s"$protocol://$host$portString$path$queryString$fragmentString"

object URL:
  /**
   * Entry point for instantiating a URL. It parses the provided [[String]].
   * @param url
   *   String parsed as URL
   * @return
   *   a [[Either]] with a String representing an error or a [[URL]] if the parsing was successful
   */
  def apply(url: String): Either[String, URL] =
    def parseQueryParams(queryString: String): Map[String, String] =
      if queryString.isEmpty then
        Map.empty[String, String]
      else
        queryString
          .drop(1)
          .split("&")
          .filter(_.contains("="))
          .map(_.split("="))
          .filter(_.length == 2)
          .collect:
            case Array(key, value) =>
              key -> value
          .toMap


    """^(https?)://([^:/?#]+)(:\d+)?([^?#]*)(\?[^#]*)?(#.*)?$""".r
      .findFirstMatchIn(url)
      .fold(Left("Invalid URL"))((matches: Regex.Match) =>
        val protocol = matches.group(1)
        val host = matches.group(2)
        val port = Option(matches.group(3)).map(_.drop(1).toInt)
        val path = Option(matches.group(4)).getOrElse("")
        val queryString = Option(matches.group(5)).getOrElse("")
        val fragment = Option(matches.group(6)).map(_.drop(1))

        Right(new URL(protocol, host, port, path, parseQueryParams(queryString), fragment))
      )

  /**
   * Used to generate an empty URL, mainly as placeholder or for testing purposes.
   * @return
   *   an empty URL
   */
  def empty: URL = new URL("", "", Option.empty, "", Map.empty, Option.empty)
