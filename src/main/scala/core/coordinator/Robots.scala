package org.unibo.scooby
package core.coordinator

import utility.http.Clients.SimpleHttpClient
import utility.http.{Request, Response, URL}

object Robots {

  def fetchRobotsTxt(siteUrl: String): String = {
    val httpClient: SimpleHttpClient = SimpleHttpClient()
    val parsedUrl = URL(siteUrl).getOrElse(URL.empty)
    // TODO: change 'https' to a method within URLs that allows the protocol
    val robotsUrl = s"${parsedUrl.toString}/robots.txt"
    Request.builder.get().at(robotsUrl).build match
      case Left(err: String) =>
        s"Error fetching robots.txt: $err"
      case Right(request: Request) =>
        request.send(httpClient) match
          case Left(s: String) =>
            s"Error while get $robotsUrl: $s"
          case Right(response: Response) =>
            response.body match {
              case None => "No content found in robots.txt"
              case Some(content: String) => content
            }
  }

  def parseRobotsTxt(robotsTxt: String): List[String] = {
    val lines = robotsTxt.split("\n")
    var userAgent: Option[String] = None
    var disallowRules: List[String] = List()

    for (line <- lines) {
      val trimmedLine = line.trim
      if (trimmedLine.startsWith("User-agent:")) {
        userAgent = Some(trimmedLine.split(":", 2)(1).trim)
      } else if (trimmedLine.startsWith("Disallow:") && userAgent.contains("*")) {
        val path = trimmedLine.split(":", 2)(1).trim
        if (path.nonEmpty) disallowRules ::= path
      }
    }

    disallowRules
  }

  def canVisit(url: String, disallowRules: List[String]): Boolean = {
    val parsedUrl = URL(url).getOrElse(URL.empty).toString
    !disallowRules.exists(rule => parsedUrl.contains(rule))
  }
}
