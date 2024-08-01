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
      val pages = List(url"http://www.google.com", url"http://www.github.com")
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
      val alreadyCrawledPages = List(url"http://www.google.com")
      val pages = List(url"https://www.google.com", url"http://www.github.com")

      coordinator ! SetCrawledPages(alreadyCrawledPages)
      coordinator ! CheckPages(pages, probe.ref)
      assert(probe.receiveMessage().result.toSet == Set("http://www.github.com").map(_.toUrl))

    "return an iterator with only pages that should be checked" in :
      val probe = testKit.createTestProbe[CrawlerCommand.CrawlerCoordinatorResponse]()
      val coordinator = testKit.spawn(Coordinator())

      coordinator ! CheckPages(List(url"http://www.google.com"), probe.ref)
      assert(probe.receiveMessage().result.toSet == Set(url"http://www.google.com"))
      coordinator ! CheckPages(List(url"https://www.google.com", url"http://www.github.com"), probe.ref)
      assert(probe.receiveMessage().result.toSet == Set(url"http://www.github.com"))

    "update the list of crawled pages" in :
      val probe = testKit.createTestProbe[List[URL]]()
      val coordinator = testKit.spawn(Coordinator())
      coordinator ! SetCrawledPages(List(url"http://www.google.com"))
      coordinator ! GetCrawledPages(probe.ref)
      probe.expectMessage(List(url"http://www.google.com"))

    "return an empty iterator when the number of pages to check is not lower than maxNumberOfLinks" in :
      val maxNumberOfLinks = 1
      val coordinator = testKit.spawn(Coordinator(maxNumberOfLinks))
      val probe = testKit.createTestProbe[CrawlerCoordinatorResponse]()
      val pages = List(url"http://example.com")

      coordinator ! CheckPages(pages, probe.ref)
      val response = probe.receiveMessage()

      assert(response.result.nonEmpty, "Excpeted a non-empty iterator")

      val otherPages = List(url"http://example2.com")
      coordinator ! CheckPages(otherPages, probe.ref)

      val response2 = probe.receiveMessage()
      assert(response2.result.isEmpty, "Expected an empty iterator when maxNumberOfLinks is not lower than the number of crawledPages")
