package org.unibo.scooby
package core.coordinator

import org.scalatest.flatspec.AnyFlatSpec
import utility.ScalaTestWithMockServer

import scala.language.{implicitConversions, postfixOps}

class RobotsTest extends ScalaTestWithMockServer:

  "fetchRobotsTxt" should "fetch the robots.txt content from a given URL" in:
    val content = Robots.fetchRobotsTxt("https://www.unibo.it/robots.txt")
    content should not be empty


  "parseRobotsTxt" should "parse disallow rules from robots.txt content" in:
    val robotsTxt =
      """
        |User-agent: *
        |Disallow: /private/
        |Disallow: /tmp/
      """.stripMargin

    val disallowRules = Robots.parseRobotsTxt(robotsTxt)
    disallowRules should contain allOf ("/private/", "/tmp/")


  "canVisit" should "return false for disallowed URLs" in:
    val disallowRules = Set("/private/", "/tmp/")

    Robots.canVisit("https://www.example.com/private/data", disallowRules) should be(false)
    Robots.canVisit("https://www.example.com/tmp/data", disallowRules) should be(false)


  it should "return true for allowed URLs" in:
    val disallowRules = Set("/private/", "/tmp/")

    Robots.canVisit("https://www.example.com/public/data", disallowRules) should be(true)
    Robots.canVisit("https://www.example.com/index.html", disallowRules) should be(true)


  "The full workflow" should "fetch, parse and check URLs correctly" in:
    val siteUrl = "http://localhost:8080"
    val robotsTxt = Robots.fetchRobotsTxt(siteUrl)
    val disallowRules = Robots.parseRobotsTxt(robotsTxt)
    val allowedUrl = "https://www.example.com/public/data"
    val disallowedUrl = "https://www.example.com/private/data"

    Robots.canVisit(allowedUrl, disallowRules) should be(true)
    Robots.canVisit(disallowedUrl, disallowRules) should be(false)


