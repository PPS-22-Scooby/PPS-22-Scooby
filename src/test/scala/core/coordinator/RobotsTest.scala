package org.unibo.scooby
package core.coordinator

import org.scalatest.flatspec.AnyFlatSpec
import org.unibo.scooby.core.coordinator.CoordinatorCommand.{CheckPages, SetupRobots}
import org.unibo.scooby.core.crawler.CrawlerCommand
import org.unibo.scooby.core.scooby.ScoobyCommand
import org.unibo.scooby.core.scooby.ScoobyCommand.RobotsChecked
import utility.ScalaTestWithMockServer
import org.unibo.scooby.utility.http.URL.toUrl

import scala.language.{implicitConversions, postfixOps}

class RobotsTest extends ScalaTestWithMockServer:

  "fetchRobotsTxt" should "fetch the robots.txt content from a given URL" in:
    val content = Robots.fetchRobotsTxt("https://www.unibo.it/robots.txt".toUrl)
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
    val siteUrl = "http://localhost:8080".toUrl
    val disallowRules = Robots.getDisallowedFromRobots(siteUrl)
    val allowedUrl = "https://www.example.com/public/data"
    val disallowedUrl = "https://www.example.com/private/data"

    Robots.canVisit(allowedUrl, disallowRules) should be(true)
    Robots.canVisit(disallowedUrl, disallowRules) should be(false)


  "The coordinator" should "disallow invalid URLs when contacted" in:
    val siteUrl = "http://localhost:8080"
    val coordinator = testKit.spawn(Coordinator())
    val robotsCheckedProbe = testKit.createTestProbe[ScoobyCommand]()
    coordinator ! SetupRobots(siteUrl.toUrl, robotsCheckedProbe.ref)
    robotsCheckedProbe.expectMessageType[ScoobyCommand.RobotsChecked]

    val crawlerResponseProbe = testKit.createTestProbe[CrawlerCommand.CrawlerCoordinatorResponse]()
    val allowedUrl = "https://www.example.com/public/data"
    val disallowedUrl = "https://www.example.com/private/data"
    coordinator ! CheckPages(List(allowedUrl.toUrl, disallowedUrl.toUrl), crawlerResponseProbe.ref)
    assert(crawlerResponseProbe.receiveMessage().result.toSet == Set(allowedUrl.toUrl))


