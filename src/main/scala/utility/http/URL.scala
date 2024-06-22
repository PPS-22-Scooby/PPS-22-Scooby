package org.unibo.scooby
package utility.http

import scala.annotation.targetName
import scala.math.Ordered
import scala.util.Try

final case class URL private(private val protocol: String,
                             private val host: String,
                             private val port: Option[Int],
                             private val path: String,
                             private val queryParams: Map[String, String],
                             private val fragment: Option[String]) extends Ordered[URL]:

  def domain: String = host + port.getOrElse("")

  def parent: URL = URL(protocol, host, port,
    path.split("/").dropRight(1).mkString("/") + "/",
    Map.empty, Option.empty)

  override def compare(that: URL): Int =
    val thisPathLength = path.split("/").length
    val thatPathLength = that.path.split("/").length
    thisPathLength compare thatPathLength


  @targetName("append")
  infix def /(other: String): URL =
    this / URL(protocol, host, port, path + other, Map.empty, Option.empty)

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
  def apply(url: String): Try[URL] =
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

    Try:
      val pattern = """^(https?)://([^:/?#]+)(:\d+)?([^?#]*)(\?[^#]*)?(#.*)?$""".r
      val matches = pattern
        .findFirstMatchIn(url)
        .getOrElse(throw new IllegalArgumentException("Invalid URL format"))

      val protocol = matches.group(1)
      val host = matches.group(2)
      val port = Option(matches.group(3)).map(_.drop(1).toInt)
      val path = Option(matches.group(4)).getOrElse("")
      val queryString = Option(matches.group(5)).getOrElse("")
      val fragment = Option(matches.group(6)).map(_.drop(1))

      new URL(protocol, host, port, path, parseQueryParams(queryString), fragment)

  def empty: URL = new URL("", "", Option.empty, "", Map.empty, Option.empty)
