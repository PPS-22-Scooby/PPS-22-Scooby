package org.unibo.scooby
package crawler

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.{ActorRef, ActorSystem}
import core.crawler.CrawlerCommand.{Crawl, CrawlerCoordinatorResponse}
import core.coordinator.{Coordinator, CoordinatorCommand}
import utility.http.URL
import core.crawler.{Crawler, CrawlerCommand}
import utility.MockServer

import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration.*
import akka.actor.typed.scaladsl.AskPattern.*
import akka.util.Timeout

import scala.language.implicitConversions

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
