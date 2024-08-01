package org.unibo.scooby
package core.coordinator

import org.unibo.scooby.utility.http.Clients.SimpleHttpClient
import org.unibo.scooby.utility.http.api.Calls.GET
import org.unibo.scooby.utility.http.{HttpError, URL}

/**
 * The `Robots` object provides functionalities to fetch and parse the robots.txt file from a given website. It also
 * includes a method to check if a given URL is allowed to be visited according to the rules specified in the robots.txt
 * file.
 */
object Robots:
  given httpClient: SimpleHttpClient = SimpleHttpClient()

  /**
   * A shortcut for {{{parseRobotsTxt(fetchRobotsTxt(rootUrl).getOrElse(""))}}}
   * @param rootUrl root URL of the site
   * @return a [[Set]] of disallowed URLs
   */
  def getDisallowedFromRobots(rootUrl: URL): Set[String] =
    parseRobotsTxt(fetchRobotsTxt(rootUrl).getOrElse(""))
  
  /**
   * Fetches the robots.txt file from the specified site URL.
   *
   * @param siteUrl
   *   The URL of the site from which to fetch the robots.txt file.
   * @return
   *   The content of the robots.txt file as an [[Option]] of [[String]]. Returns [[None]] if Robots.txt isn't found
   *   or if its content is empty
   */
  def fetchRobotsTxt(siteUrl: URL): Option[String] =
    val robotsUrl = siteUrl / "robots.txt"
    val fetchResult: Either[HttpError, Option[String]] = GET(robotsUrl)
    fetchResult match
      case Left(_) =>
        // Error fetching robots.txt
        Option.empty
      case Right(optionalContent: Option[String]) => optionalContent

  /**
   * Parses the content of a robots.txt file and extracts the disallow rules.
   *
   * @param robotsTxt
   *   The content of the robots.txt file as a String.
   * @return
   *   A [[Set]] of disallowed paths for the user-agent "*".
   */
  def parseRobotsTxt(robotsTxt: String): Set[String] =
    if robotsTxt.isEmpty then return Set.empty
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
   * Checks if a given string URL is allowed to be visited according to the disallow rules.
   *
   * @param url
   *   The URL to check.
   * @param disallowRules
   *   A List of disallowed paths.
   * @return
   *   `true` if the URL is allowed to be visited, `false` otherwise.
   */
  def canVisit(url: String, disallowRules: Set[String]): Boolean =
    val parsedUrl = URL(url).toString
    !disallowRules.exists(rule => parsedUrl.contains(rule))
