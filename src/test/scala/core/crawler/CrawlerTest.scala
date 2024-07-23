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

import akka.actor.testkit.typed.CapturedLogEvent
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
import org.unibo.scooby.utility.ScalaTestWithMockServer

class CrawlerTest extends ScalaTestWithMockServer:

  def buildCrawler(
                    coordinator: ActorRef[CoordinatorCommand],
                    policy: ExplorationPolicy = ExplorationPolicies.allLinks
                  ): Behavior[CrawlerCommand] =
    val exporterProbe = testKit.createTestProbe[ExporterCommands]()
    Crawler(coordinator, exporterProbe.ref, _.content, policy, 2)


  "Crawler" should "send CheckPages message to Coordinator when Crawl message is received" in :
    val coordinatorProbe = testKit.createTestProbe[CoordinatorCommand]()
    val crawler = testKit.spawn(buildCrawler(coordinatorProbe.ref))
    val url = URL("http://localhost:8080").getOrElse(fail("Invalid URL"))
    crawler ! Crawl(url)
    coordinatorProbe.expectMessage(CoordinatorCommand.CheckPages(List(url"https://www.fortest.it".getOrElse(URL.empty)), crawler))

  it should "spawn a new Crawler actor for each link received" in :
    val coordinatorProbe = testKit.createTestProbe[CoordinatorCommand]()
    val behaviorTestKit = BehaviorTestKit(buildCrawler(coordinatorProbe.ref))

    val linksMap = Iterator(
      url"https://www.facebook.it".getOrElse(URL.empty),
      url"https://www.google.com".getOrElse(URL.empty)
    )

    behaviorTestKit.run(CrawlerCommand.CrawlerCoordinatorResponse(linksMap))

    behaviorTestKit.expectEffectType[Spawned[Crawler[String]]]

    val child1Inbox = behaviorTestKit.childInbox[CrawlerCommand]("crawler-www.facebook.it")
    val child2Inbox = behaviorTestKit.childInbox[CrawlerCommand]("crawler-www.google.com")

    child1Inbox.expectMessage(Crawl(URL("https://www.facebook.it").getOrElse(fail("Invalid URL"))))
    child2Inbox.expectMessage(Crawl(URL("https://www.google.com").getOrElse(fail("Invalid URL"))))

  it should "log an error message when the URL can't be parsed" in :
    val coordinatorProbe = testKit.createTestProbe[CoordinatorCommand]()
    val behaviorTestKit = BehaviorTestKit(buildCrawler(coordinatorProbe.ref))

    val url = URL("http://localhost:23111").getOrElse(fail("Invalid URL"))
    behaviorTestKit.run(CrawlerCommand.Crawl(URL("http://localhost:23111").getOrElse(fail("Invalid URL"))))

    behaviorTestKit.logEntries() shouldBe Seq(
      CapturedLogEvent(Level.ERROR, f"Error while crawling $url: Exception when sending request: GET $url")
    )

  it should "handle CrawlerCoordinatorResponse correctly when the same link is sent twice" in :
    val coordinatorProbe = testKit.createTestProbe[CoordinatorCommand]()
    val behaviorTestKit = BehaviorTestKit(buildCrawler(coordinatorProbe.ref))

    val url = URL("http://localhost:8080").getOrElse(fail("Invalid URL"))

    behaviorTestKit.run(CrawlerCommand.Crawl(url))
    behaviorTestKit.run(CrawlerCommand.CrawlerCoordinatorResponse(Iterator(url)))

    behaviorTestKit.expectEffectType[Spawned[Crawler[String]]]
    val childInbox = behaviorTestKit.childInbox[CrawlerCommand](s"crawler-${url.withoutProtocol}")
    childInbox.expectMessage(CrawlerCommand.Crawl(url))

    behaviorTestKit.run(CrawlerCommand.CrawlerCoordinatorResponse(Iterator.empty))

  it should "explore only links with the same domain if exploration strategy is sameDomainLinks" in:
    val coordinatorProbe = testKit.createTestProbe[CoordinatorCommand]()
    val crawler = testKit.spawn(buildCrawler(coordinatorProbe.ref, ExplorationPolicies.sameDomainLinks))

    val url = url"http://localhost:8080/ext-url".getOrElse(fail("Invalid URL"))
    crawler ! Crawl(url)

    coordinatorProbe.expectMessage(CoordinatorCommand.CheckPages(List(
      url"http://localhost:8080/a".getOrElse(URL.empty),
      url"http://localhost:8080/b".getOrElse(URL.empty)
    ), crawler))
  
  it should "explore all links if exploration strategy is allLinks" in:
    val coordinatorProbe = testKit.createTestProbe[CoordinatorCommand]()
    val crawler = testKit.spawn(buildCrawler(coordinatorProbe.ref, ExplorationPolicies.allLinks))

    val url = url"http://localhost:8080/ext-url".getOrElse(fail("Invalid URL"))
    crawler ! Crawl(url)

    coordinatorProbe.expectMessage(CoordinatorCommand.CheckPages(List(
      url"http://localhost:8080/a".getOrElse(URL.empty),
      url"http://localhost:8080/b".getOrElse(URL.empty),
      url"http://www.external-url.it".getOrElse(URL.empty)
    ), crawler))