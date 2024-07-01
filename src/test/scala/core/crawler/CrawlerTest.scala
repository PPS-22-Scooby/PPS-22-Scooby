package org.unibo.scooby
package core.crawler

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, BehaviorTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.{ActorRef, ActorSystem}
import core.crawler.CrawlerCommand.{Crawl, CrawlerCoordinatorResponse}
import core.coordinator.CoordinatorCommand
import utility.http.URL
import utility.MockServer

import akka.actor.testkit.typed.CapturedLogEvent
import akka.actor.testkit.typed.Effect.*
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration.*
import akka.actor.typed.scaladsl.AskPattern.*
import akka.util.Timeout
import org.slf4j.event.Level

import scala.language.{implicitConversions, postfixOps}

class CrawlerTest extends AnyFlatSpec, Matchers, BeforeAndAfterAll:
  implicit val timeout: Timeout = 30.seconds

  val testKit: ActorTestKit = ActorTestKit()

  implicit val system: ActorSystem[Nothing] = testKit.system
  val crawlerProbe: ActorRef[CoordinatorCommand] = testKit.createTestProbe[CoordinatorCommand]().ref

  val webServerSystem: ActorSystem[MockServer.Command] = ActorSystem(MockServer(), "WebServerSystem")

  override def beforeAll(): Unit = {
    val startFuture = webServerSystem.ask[MockServer.Command](ref => MockServer.Start(ref))(timeout, system.scheduler)
    val result = Await.result(startFuture, timeout.duration)
    result shouldBe MockServer.ServerStarted
  }

  override def afterAll(): Unit = {
    webServerSystem ! MockServer.Stop
    testKit.shutdownTestKit()
  }

  "Crawler" should "send CheckPages message to Coordinator when Crawl message is received" in :
    val coordinatorProbe = testKit.createTestProbe[CoordinatorCommand]()
    val crawler = testKit.spawn(Crawler(coordinatorProbe.ref))
    val url = URL("http://localhost:8080").getOrElse(fail("Invalid URL"))
    crawler ! Crawl(url)
    coordinatorProbe.expectMessage(CoordinatorCommand.CheckPages(List("https://www.fortest.it"), crawler))

  it should "spawn a new Crawler actor for each link received" in :
    val coordinatorProbe = testKit.createTestProbe[CoordinatorCommand]()
    val behaviorTestKit = BehaviorTestKit(Crawler(coordinatorProbe.ref))

    val linksMap = Map("https://www.facebook.it" -> true, "https://www.google.com" -> true)
    behaviorTestKit.run(CrawlerCommand.CrawlerCoordinatorResponse(linksMap))

    behaviorTestKit.expectEffectType[Spawned[Crawler]]

    val child1Inbox = behaviorTestKit.childInbox[CrawlerCommand]("crawler-www.facebook.it")
    val child2Inbox = behaviorTestKit.childInbox[CrawlerCommand]("crawler-www.google.com")

    child1Inbox.expectMessage(Crawl(URL("https://www.facebook.it").getOrElse(fail("Invalid URL"))))
    child2Inbox.expectMessage(Crawl(URL("https://www.google.com").getOrElse(fail("Invalid URL"))))

  it should "log an error message when the URL can't be parsed" in :
    val coordinatorProbe = testKit.createTestProbe[CoordinatorCommand]()
    val behaviorTestKit = BehaviorTestKit(Crawler(coordinatorProbe.ref))

    val url = URL("http://localhost:23111").getOrElse(fail("Invalid URL"))
    behaviorTestKit.run(CrawlerCommand.Crawl(URL("http://localhost:23111").getOrElse(fail("Invalid URL"))))

    behaviorTestKit.logEntries() shouldBe Seq(
      CapturedLogEvent(Level.ERROR, f"Error while crawling $url: Exception when sending request: GET $url")
    )