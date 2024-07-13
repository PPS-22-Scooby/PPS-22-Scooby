package org.unibo.scooby
package core.crawler

import core.coordinator.CoordinatorCommand
import core.crawler.CrawlerCommand.*
import core.exporter.{Exporter, ExporterCommands}
import core.scraper.{Scraper, ScraperPolicies}
import utility.ScalaTestWithMockServer
import utility.document.ScrapeDocument
import utility.http.URL

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import org.unibo.scooby.core.scraper.ScraperPolicies.ScraperPolicy

class CrawlerLifecyleTest extends ScalaTestWithMockServer:

  val scraperRulePlaceholder: ScraperPolicy[ScrapeDocument, String] = ScraperPolicies.scraperRule(Seq("body"), "tag")

  "Crawler" should "die if there is no valid link" in:
    val mockedBehavior: Behavior[CoordinatorCommand] = Behaviors.receiveMessage:
      case CoordinatorCommand.CheckPages(links, replyTo) if links.nonEmpty && links.head == "https://www.fortest.it" =>
        replyTo ! CrawlerCoordinatorResponse(List("http://localhost:8080/notFound").iterator)
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
      _.frontier.map(URL(_).getOrElse(URL.empty)),
      2
    ))
    val url = URL("http://localhost:8080").getOrElse(fail("Invalid URL"))
    crawler ! Crawl(url)
    coordinatorProbe.expectMessage(CoordinatorCommand.CheckPages(List("https://www.fortest.it"), crawler))

    coordinatorProbe.expectTerminated(crawler.ref)

  "Crawler" should "die if max depth is reached" in:
    var count: Int = 0
    val mockedBehavior: Behavior[CoordinatorCommand] = Behaviors.receiveMessage:
      case CoordinatorCommand.CheckPages(links, replyTo) =>
        replyTo ! CrawlerCoordinatorResponse(List("http://localhost:8080/notFound").iterator)
        count += 1
        Behaviors.same

    val coordinatorProbe = testKit.createTestProbe[CoordinatorCommand]()
    val mockedCoordinator = testKit.spawn(Behaviors.monitor(coordinatorProbe.ref, mockedBehavior))
    val exporterProbe = testKit.createTestProbe[ExporterCommands]()

    val crawler = testKit.spawn(Crawler(
      mockedCoordinator,
      exporterProbe.ref,
      scraperRulePlaceholder,
      _.frontier.map(URL(_).getOrElse(URL.empty)),
      1
    ))
    val url = URL("http://localhost:8080").getOrElse(fail("Invalid URL"))
    crawler ! Crawl(url)
    coordinatorProbe.expectTerminated(crawler.ref)
    count should be(1)

    count = 0
    val crawler2 = testKit.spawn(Crawler(
      mockedCoordinator,
      exporterProbe.ref,
      scraperRulePlaceholder,
      _.frontier.map(URL(_).getOrElse(URL.empty)),
      2
    ))

    crawler2 ! Crawl(url)
    coordinatorProbe.expectTerminated(crawler2.ref)
    count should be(2)





