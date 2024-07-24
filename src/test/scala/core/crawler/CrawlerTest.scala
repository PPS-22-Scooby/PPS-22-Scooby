package org.unibo.scooby
package core.crawler

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, BehaviorTestKit}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import core.crawler.CrawlerCommand.{Crawl, CrawlerCoordinatorResponse}
import core.coordinator.CoordinatorCommand
import utility.http.URL
import utility.http.URL.*
import utility.MockServer

import akka.actor.testkit.typed.{CapturedLogEvent, Effect}
import akka.actor.testkit.typed.Effect.*
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration.*
import akka.actor.typed.scaladsl.AskPattern.*
import akka.util.Timeout
import org.slf4j.event.Level
import core.exporter.ExporterCommands
import utility.document.ScrapeDocument

import scala.language.{implicitConversions, postfixOps}

class CrawlerTest extends AnyFlatSpec, Matchers, BeforeAndAfterAll:
  implicit val timeout: Timeout = 30.seconds

  val testKit: ActorTestKit = ActorTestKit()

  implicit val system: ActorSystem[Nothing] = testKit.system
  val crawlerProbe: ActorRef[CoordinatorCommand] = testKit.createTestProbe[CoordinatorCommand]().ref

  val webServerSystem: ActorSystem[MockServer.Command] = ActorSystem(MockServer(), "WebServerSystem")

  override def beforeAll(): Unit =
    val startFuture = webServerSystem.ask[MockServer.Command](ref => MockServer.Start(ref))(timeout, system.scheduler)
    val result = Await.result(startFuture, timeout.duration)
    result shouldBe MockServer.ServerStarted

  override def afterAll(): Unit =
    webServerSystem ! MockServer.Stop
    testKit.shutdownTestKit()

  def buildCrawler(
                    coordinator: ActorRef[CoordinatorCommand],
                    policy: ExplorationPolicy = ExplorationPolicies.allLinks
                  ): Behavior[CrawlerCommand] =
    val exporterProbe = testKit.createTestProbe[ExporterCommands]()
    Crawler(coordinator, exporterProbe.ref, _.content, policy, 2)


  "Crawler" should "send CheckPages message to Coordinator when Crawl message is received" in :
    val coordinatorProbe = testKit.createTestProbe[CoordinatorCommand]()
    val crawler = testKit.spawn(buildCrawler(coordinatorProbe.ref))
    val url = URL("http://localhost:8080")
    crawler ! Crawl(url)
    coordinatorProbe.expectMessage(CoordinatorCommand.CheckPages(List(url"https://www.fortest.it"), crawler))

  it should "spawn a new Crawler actor for each link received" in :
    val coordinatorProbe = testKit.createTestProbe[CoordinatorCommand]()
    val behaviorTestKit = BehaviorTestKit(buildCrawler(coordinatorProbe.ref))

    val links = List(
      url"https://www.facebook.it",
      url"https://www.google.com"
    )

    behaviorTestKit.run(CrawlerCommand.CrawlerCoordinatorResponse(links.iterator))

    val expectedMessages = links.map(Crawl(_))
    val effects = behaviorTestKit.retrieveAllEffects()
    effects.foreach:
      case Effect.WatchedWith(actorRef: ActorRef[_], _) =>
        val childInbox = behaviorTestKit.childInbox(actorRef)
        assert(childInbox.hasMessages)
        assert(childInbox.receiveAll().exists(expectedMessages.contains))
      case _ => println(f"Other effect")

  it should "log an error message when the URL can't be parsed" in :
    val coordinatorProbe = testKit.createTestProbe[CoordinatorCommand]()
    val behaviorTestKit = BehaviorTestKit(buildCrawler(coordinatorProbe.ref))

    val url = URL("http://localhost:23111")
    behaviorTestKit.run(CrawlerCommand.Crawl(URL("http://localhost:23111")))

    behaviorTestKit.logEntries() shouldBe Seq(
      CapturedLogEvent(Level.ERROR, f"Error while crawling $url: Exception when sending request: GET $url")
    )

  it should "explore only links with the same domain if exploration strategy is sameDomainLinks" in:
    val coordinatorProbe = testKit.createTestProbe[CoordinatorCommand]()
    val crawler = testKit.spawn(buildCrawler(coordinatorProbe.ref, ExplorationPolicies.sameDomainLinks))

    val url = url"http://localhost:8080/ext-url"
    crawler ! Crawl(url)

    coordinatorProbe.expectMessage(CoordinatorCommand.CheckPages(List(
      url"http://localhost:8080/a",
      url"http://localhost:8080/b"
    ), crawler))
  
  it should "explore all links if exploration strategy is allLinks" in:
    val coordinatorProbe = testKit.createTestProbe[CoordinatorCommand]()
    val crawler = testKit.spawn(buildCrawler(coordinatorProbe.ref, ExplorationPolicies.allLinks))

    val url = url"http://localhost:8080/ext-url"
    crawler ! Crawl(url)

    coordinatorProbe.expectMessage(CoordinatorCommand.CheckPages(List(
      url"http://localhost:8080/a",
      url"http://localhost:8080/b",
      url"http://www.external-url.it"
    ), crawler))