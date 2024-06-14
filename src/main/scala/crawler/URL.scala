package org.unibo.scooby
package crawler

import scala.annotation.targetName
import scala.util.Try

final case class URL(
  protocol: String,
  host: String,
  port: Option[Int],
  path: String,
  queryParams: Map[String, String],
  fragment: Option[String]
) extends Ordered[URL]:

  @targetName("append")
  infix def /(other: String): URL =
    URL(protocol, host, port, path + other, Map.empty, Option.empty)

  override def compare(that: URL): Int =
    val thisPathLength = path.split("/").length
    val thatPathLength = that.path.split("/").length
    thisPathLength compare thatPathLength

  override def toString: String =
    val portString = port.map(":" + _).getOrElse("")
    val queryString =
      if (queryParams.isEmpty) ""
      else "?" + queryParams.map { case (k, v) => s"$k=$v" }.mkString("&")
    val fragmentString = fragment.map("#" + _).getOrElse("")
    s"$protocol://$host$portString$path$queryString$fragmentString"

object URL {
  def apply(url: String): Try[URL] = Try {
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

    val queryParams = if (queryString.isEmpty) {
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

    new URL(protocol, host, port, path, queryParams, fragment)
  }
}
