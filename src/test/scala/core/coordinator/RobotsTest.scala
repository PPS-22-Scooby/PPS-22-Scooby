package org.unibo.scooby
package core.coordinator

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import utility.MockServer
import scala.concurrent.Await
import scala.language.{implicitConversions, postfixOps}
import scala.concurrent.duration.DurationInt
import akka.actor.typed.scaladsl.AskPattern.*
import org.scalatest.BeforeAndAfterAll

class RobotsTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  implicit val timeout: Timeout = 30.seconds

  val testKit: ActorTestKit = ActorTestKit()

  implicit val system: ActorSystem[Nothing] = testKit.system

  val webServerSystem: ActorSystem[MockServer.Command] = ActorSystem(MockServer(), "WebServerSystem")

  override def beforeAll(): Unit =
    val startFuture = webServerSystem.ask[MockServer.Command](ref => MockServer.Start(ref))(timeout, system.scheduler)
    val result = Await.result(startFuture, timeout.duration)
    result shouldBe MockServer.ServerStarted

  override def afterAll(): Unit =
    webServerSystem ! MockServer.Stop
    testKit.shutdownTestKit()

  "fetchRobotsTxt" should "fetch the robots.txt content from a given URL" in {
    val content = Robots.fetchRobotsTxt("https://www.unibo.it/robots.txt")
    content should not be empty
  }

  "parseRobotsTxt" should "parse disallow rules from robots.txt content" in {
    val robotsTxt =
      """
        |User-agent: *
        |Disallow: /private/
        |Disallow: /tmp/
      """.stripMargin

    val disallowRules = Robots.parseRobotsTxt(robotsTxt)
    disallowRules should contain allOf ("/private/", "/tmp/")
  }

  "canVisit" should "return false for disallowed URLs" in {
    val disallowRules = Set("/private/", "/tmp/")

    Robots.canVisit("https://www.example.com/private/data", disallowRules) should be(false)
    Robots.canVisit("https://www.example.com/tmp/data", disallowRules) should be(false)
  }

  it should "return true for allowed URLs" in {
    val disallowRules = Set("/private/", "/tmp/")

    Robots.canVisit("https://www.example.com/public/data", disallowRules) should be(true)
    Robots.canVisit("https://www.example.com/index.html", disallowRules) should be(true)
  }

  "The full workflow" should "fetch, parse and check URLs correctly" in {
    val siteUrl = "http://localhost:8080"
    val robotsTxt = Robots.fetchRobotsTxt(siteUrl)
    val disallowRules = Robots.parseRobotsTxt(robotsTxt)
    val allowedUrl = "https://www.example.com/public/data"
    val disallowedUrl = "https://www.example.com/private/data"

    Robots.canVisit(allowedUrl, disallowRules) should be(true)
    Robots.canVisit(disallowedUrl, disallowRules) should be(false)
  }
}
