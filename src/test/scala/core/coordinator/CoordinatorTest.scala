package org.unibo.scooby
package core.coordinator

import core.coordinator.CoordinatorCommand.*

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import core.crawler.CrawlerCommand
import core.crawler.CrawlerCommand.CrawlerCoordinatorResponse
import utility.http.URL.*
import utility.http.URL

class CoordinatorTest extends AnyWordSpecLike with BeforeAndAfterAll :

  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "A Coordinator" must :
    "return an iterator of pages and their crawled status" in :
      val probe = testKit.createTestProbe[CrawlerCommand.CrawlerCoordinatorResponse]()
      val coordinator = testKit.spawn(Coordinator())
      val pages = List(url"http://www.google.com".getOrElse(empty), url"http://www.github.com".getOrElse(empty))
      coordinator ! CheckPages(pages, probe.ref)
      assert(probe.receiveMessage().result.toSet == pages.toSet)

    "return an empty iterator when no pages are provided" in :
      val probe = testKit.createTestProbe[CrawlerCommand.CrawlerCoordinatorResponse]()
      val coordinator = testKit.spawn(Coordinator())
      coordinator ! CheckPages(List.empty, probe.ref)
      assert(probe.receiveMessage().result.toSet == Set.empty)

    "return an iterator without pages that have been crawled" in :
      val probe = testKit.createTestProbe[CrawlerCommand.CrawlerCoordinatorResponse]()
      val coordinator = testKit.spawn(Coordinator())
      val alreadyCrawledPages = List(url"http://www.google.com".getOrElse(empty))
      val pages = List(url"https://www.google.com".getOrElse(empty), url"http://www.github.com".getOrElse(empty))

      coordinator ! SetCrawledPages(alreadyCrawledPages)
      coordinator ! CheckPages(pages, probe.ref)
      assert(probe.receiveMessage().result.toSet == Set("http://www.github.com").map(_.toUrl.getOrElse(empty)))

    "return an iterator with only pages that should be checked" in :
      val probe = testKit.createTestProbe[CrawlerCommand.CrawlerCoordinatorResponse]()
      val coordinator = testKit.spawn(Coordinator())

      coordinator ! CheckPages(List(url"http://www.google.com".getOrElse(empty)), probe.ref)
      assert(probe.receiveMessage().result.toSet == Set(url"http://www.google.com".getOrElse(empty)))
      coordinator ! CheckPages(List(url"https://www.google.com".getOrElse(empty), url"http://www.github.com".getOrElse(empty)), probe.ref)
      assert(probe.receiveMessage().result.toSet == Set(url"http://www.github.com".getOrElse(empty)))

    "update the list of crawled pages" in :
      val probe = testKit.createTestProbe[List[URL]]()
      val coordinator = testKit.spawn(Coordinator())
      coordinator ! SetCrawledPages(List(url"http://www.google.com".getOrElse(empty)))
      coordinator ! GetCrawledPages(probe.ref)
      probe.expectMessage(List(url"http://www.google.com".getOrElse(empty)))

    "return an empty iterator when the number of pages to check is not lower than maxNumberOfLinks" in :
      val maxNumberOfLinks = 5
      val coordinator = testKit.spawn(Coordinator(maxNumberOfLinks))
      val probe = testKit.createTestProbe[CrawlerCoordinatorResponse]()
      val pages = List.fill(maxNumberOfLinks + 1)(url"http://example.com".getOrElse(empty))

      coordinator ! CheckPages(pages, probe.ref)
      val response = probe.receiveMessage()

      assert(response.result.isEmpty, "Expected an empty iterator when maxNumberOfLinks is not lower than the number of crawledPages")