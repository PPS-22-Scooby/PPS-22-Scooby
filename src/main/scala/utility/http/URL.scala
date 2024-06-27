package org.unibo.scooby
package utility.http

import scala.annotation.targetName
import scala.math.Ordered
import scala.util.Try
import scala.util.matching.Regex

/**
 * Class that represents a URL used for HTTP calls.
 * @param protocol protocol used (e.g. "https")
 * @param host host of the URL (e.g. "www.google.com")
 * @param port port of the URL. [[Some]] containing the port or [[Empty]] if the URL doesn't contain a port.
 * @param path path of the URL (e.g. "/example/path")
 * @param queryParams [[Map]] that contains the query params of the URL (e.g. "service=mail&passive=true")
 * @param fragment fragment of the URL. [[Some]] containing the fragment (e.g. "#section") or [[None]] if there is no
 *                 fragment
 */
final case class URL private(private val protocol: String,
                             private val host: String,
                             private val port: Option[Int],
                             private val path: String,
                             private val queryParams: Map[String, String],
                             private val fragment: Option[String]) extends Ordered[URL]:

  /**
   * Gets the domain from the URL
   * @return a [[String]] containing the domain
   */
  def domain: String = host + port.getOrElse("")

  /**
   * Gets the "parent" of this URL, that is the preceding path (e.g. the parent of "/example/path" is "/example/")
   * @return the parent [[URL]] of this URL.
   */
  def parent: URL = URL(protocol, host, port,
    path.split("/").dropRight(1).mkString("/") + "/",
    Map.empty, Option.empty)

  /**
   * Compares the depth of two URLs. Returns a value greater that 0 if this URL is has greater depth, equal to 0 if they
   * have the same depth, less than 0 otherwise
   * @param that [[URL]] to compare with this URL
   * @return a [[Int]] value representing the result of the comparison
   */
  override def compare(that: URL): Int =
    val thisPathLength = path.split("/").length
    val thatPathLength = that.path.split("/").length
    thisPathLength compare thatPathLength

  /**
   * Utility method that appends to this URL a [[String]], placing the trailing backslashes correctly
   * @param other the path (e.g. "/example") to be appended to this URL
   * @return a new [[URL]] with the new path appended
   */
  @targetName("append")
  infix def /(other: String): URL =
    this / URL(protocol, host, port, path + other, Map.empty, Option.empty)

  /**
   * Utility method that appends an another [[URL]] to this URL, with the same protocol, host and post,
   * placing the trailing backslashes correctly
   * @param other URL to be appended
   * @return a new [[URL]] with the new path appended
   */
  @targetName("append")
  infix def /(other: URL): URL =
    def removeLeadingTrailingSlashes(input: String): String =
      input.replaceAll("^/+", "").replaceAll("/+$", "")

    if other.protocol == protocol && other.host == host && other.port == port then
    // warning: not giving info about malformed URLs -> just returning itself if fails
      URL.apply(URL(protocol, host, port,
        removeLeadingTrailingSlashes(path) + "/" + removeLeadingTrailingSlashes(other.path),
        Map.empty, Option.empty).toString).getOrElse(this)
    else
      this

  override def toString: String =
    val portString = port.map(":" + _).getOrElse("")
    val queryString =
      if (queryParams.isEmpty) ""
      else "?" + queryParams.map { case (k, v) => s"$k=$v" }.mkString("&")
    val fragmentString = fragment.map("#" + _).getOrElse("")
    s"$protocol://$host$portString$path$queryString$fragmentString"

object URL:
  /**
   * Entry point for instantiating a URL. It parses the provided [[String]].
   * @param url String parsed as URL
   * @return a [[Try]] with `Success` if the provided string was formatted correctly, `Failure` otherwise.
   */
  def apply(url: String): Either[String, URL] =
    def parseQueryParams(queryString: String): Map[String, String] =
      if (queryString.isEmpty) {
        Map.empty[String, String]
      } else {
        queryString
          .drop(1)
          .split("&")
          .collect {
            case param if param.contains("=") =>
              val Array(key, value) = param.split("=")
              key -> value
          }
          .toMap
      }
      
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
   * @return an empty URL
   */
  def empty: URL = new URL("", "", Option.empty, "", Map.empty, Option.empty)
