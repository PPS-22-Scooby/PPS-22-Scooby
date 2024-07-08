package org.unibo.scooby
package core.coordinator

import utility.http.Clients.SimpleHttpClient
import utility.http.{HttpError, Request, Response, URL}

/**
 * The `Robots` object provides functionalities to fetch and parse the robots.txt file from a given website. It also
 * includes a method to check if a given URL is allowed to be visited according to the rules specified in the robots.txt
 * file.
 */
object Robots:

  /**
   * Fetches the robots.txt file from the specified site URL.
   *
   * @param siteUrl
   *   The URL of the site from which to fetch the robots.txt file.
   * @return
   *   The content of the robots.txt file as a String. Returns an error message if fetching fails.
   */
  def fetchRobotsTxt(siteUrl: String): String =
    val httpClient: SimpleHttpClient = SimpleHttpClient()
    val parsedUrl = URL(siteUrl).getOrElse(URL.empty)
    // TODO: change 'https' to a method within URLs that allows the protocol
    val robotsUrl = s"${parsedUrl.toString}/robots.txt"
    Request.builder.get().at(robotsUrl).build match
      case Left(err: HttpError) =>
        s"Error fetching robots.txt: ${err.message}"
      case Right(request: Request) =>
        request.send(httpClient) match
          case Left(s: HttpError) =>
            s"Error while get $robotsUrl: ${s.message}"
          case Right(response: Response) =>
            response.body match
              case None => "No content found in robots.txt"
              case Some(content: String) => content

  /**
   * Parses the content of a robots.txt file and extracts the disallow rules.
   *
   * @param robotsTxt
   *   The content of the robots.txt file as a String.
   * @return
   *   A List of disallowed paths for the user-agent "*".
   */
  def parseRobotsTxt(robotsTxt: String): Set[String] =
    val lines = robotsTxt.split("\n").toList
    val initialState = (Option.empty[String], List.empty[String])

    val (_, disallowRules) = lines.foldLeft(initialState):
      case ((userAgent, disallowRules), line) =>
        val trimmedLine = line.trim
        if (trimmedLine.startsWith("#"))
          // Skip comments
          (userAgent, disallowRules)
        else
          trimmedLine.split(":", 2) match
            case Array("User-agent", ua) =>
              (Some(ua.trim), disallowRules)
            case Array("Disallow", path) if userAgent.contains("*") && path.trim.nonEmpty =>
              (userAgent, path.trim :: disallowRules)
            case _ =>
              (userAgent, disallowRules)
    disallowRules.toSet


  /**
   * Checks if a given URL is allowed to be visited according to the disallow rules.
   *
   * @param url
   *   The URL to check.
   * @param disallowRules
   *   A List of disallowed paths.
   * @return
   *   `true` if the URL is allowed to be visited, `false` otherwise.
   */
  def canVisit(url: String, disallowRules: Set[String]): Boolean =
    val parsedUrl = URL(url).getOrElse(URL.empty).toString
    !disallowRules.exists(rule => parsedUrl.contains(rule))
