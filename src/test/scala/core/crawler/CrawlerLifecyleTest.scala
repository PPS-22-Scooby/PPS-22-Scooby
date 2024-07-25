package org.unibo.scooby
package core.crawler

import core.coordinator.CoordinatorCommand
import core.crawler.CrawlerCommand.*
import core.exporter.ExporterCommands
import core.scraper.ScraperPolicies
import core.scraper.ScraperPolicies.ScraperPolicy
import utility.ScalaTestWithMockServer
import utility.document.ScrapeDocument
import utility.http.URL
import utility.http.URL.*

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

class CrawlerLifecyleTest extends ScalaTestWithMockServer:

  val scraperRulePlaceholder: ScraperPolicy[String] = ScraperPolicies.scraperRule(Seq("body"), "tag")

  "Crawler" should "die if there is no valid link" in:
    val mockedBehavior: Behavior[CoordinatorCommand] = Behaviors.receiveMessage:
      case CoordinatorCommand.CheckPages(links, replyTo) if links.nonEmpty && links.head == url"https://www.fortest.it" =>
        replyTo ! CrawlerCoordinatorResponse(List(url"http://localhost:8080/notFound").iterator)
        Behaviors.same
      case CoordinatorCommand.CheckPages(links, replyTo) =>
        replyTo ! CrawlerCoordinatorResponse(Iterator.empty)
        Behaviors.same

    val coordinatorProbe = testKit.createTestProbe[CoordinatorCommand]()
    val mockedCoordinator = testKit.spawn(Behaviors.monitor(coordinatorProbe.ref, mockedBehavior))
    val exporterProbe = testKit.createTestProbe[ExporterCommands]()

    val crawler = testKit.spawn(Crawler(
      mockedCoordinator,
      exporterProbe.ref,
      scraperRulePlaceholder,
      _.frontier,
      2
    ))
    val url = url"http://localhost:8080"
    crawler ! Crawl(url)
    coordinatorProbe.expectMessage(CoordinatorCommand.CheckPages(List(url"https://www.fortest.it"), crawler))

    coordinatorProbe.expectTerminated(crawler.ref)


  "Crawler" should "die if max depth is reached" in:
    var count: Int = 0
    val mockedBehavior: Behavior[CoordinatorCommand] = Behaviors.receiveMessage:
      case CoordinatorCommand.CheckPages(links, replyTo) =>
        replyTo ! CrawlerCoordinatorResponse(List(url"http://localhost:8080/notFound").iterator)
        count += 1
        Behaviors.same

    val coordinatorProbe = testKit.createTestProbe[CoordinatorCommand]()
    val mockedCoordinator = testKit.spawn(Behaviors.monitor(coordinatorProbe.ref, mockedBehavior))
    val exporterProbe = testKit.createTestProbe[ExporterCommands]()

    val crawler = testKit.spawn(Crawler(
      mockedCoordinator,
      exporterProbe.ref,
      scraperRulePlaceholder,
      ExplorationPolicies.allLinks,
      1
    ))
    val url = url"http://localhost:8080"
    crawler ! Crawl(url)
    coordinatorProbe.expectTerminated(crawler.ref)
    count should be(1)

    count = 0
    val crawler2 = testKit.spawn(Crawler(
      mockedCoordinator,
      exporterProbe.ref,
      scraperRulePlaceholder,
      ExplorationPolicies.allLinks,
      2
    ))

    crawler2 ! Crawl(url)
    coordinatorProbe.expectTerminated(crawler2.ref)
    count should be(2)






