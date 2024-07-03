package org.unibo.scooby
package core.coordinator

import core.coordinator.CoordinatorCommand.*

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import core.crawler.CrawlerCommand
import core.crawler.CrawlerCommand.CrawlerCoordinatorResponse

class CoordinatorTest extends AnyWordSpecLike with BeforeAndAfterAll :

  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "A Coordinator" must :
    "return an iterator of pages and their crawled status" in :
      val probe = testKit.createTestProbe[CrawlerCommand.CrawlerCoordinatorResponse]()
      val coordinator = testKit.spawn(Coordinator())
      coordinator ! CheckPages(List("http://www.google.com", "http://www.github.com"), probe.ref)
      assert(probe.receiveMessage().result.toSet == Set("http://www.google.com", "http://www.github.com"))


    "return an empty iterator when no pages are provided" in :
      val probe = testKit.createTestProbe[CrawlerCommand.CrawlerCoordinatorResponse]()
      val coordinator = testKit.spawn(Coordinator())
      coordinator ! CheckPages(List.empty, probe.ref)
      assert(probe.receiveMessage().result.toSet == Set.empty)


    "return an iterator without pages that have been crawled" in :
      val probe = testKit.createTestProbe[CrawlerCommand.CrawlerCoordinatorResponse]()
      val coordinator = testKit.spawn(Coordinator())
      coordinator ! SetCrawledPages(List("http://www.google.com"))
      coordinator ! CheckPages(List("https://www.google.com", "http://www.github.com"), probe.ref)
      assert(probe.receiveMessage().result.toSet == Set("http://www.github.com"))


    "return an iterator with true values for pages that have been checked" in :
      val probe = testKit.createTestProbe[CrawlerCommand.CrawlerCoordinatorResponse]()
      val coordinator = testKit.spawn(Coordinator())
      coordinator ! CheckPages(List("http://www.google.com"), probe.ref)
      assert(probe.receiveMessage().result.toSet == Set("http://www.google.com"))
      coordinator ! CheckPages(List("https://www.google.com", "http://www.github.com"), probe.ref)
      assert(probe.receiveMessage().result.toSet == Set("http://www.github.com"))


    "update the list of crawled pages" in :
      val probe = testKit.createTestProbe[List[String]]()
      val coordinator = testKit.spawn(Coordinator())
      coordinator ! SetCrawledPages(List("http://www.google.com"))
      coordinator ! GetCrawledPages(probe.ref)
      probe.expectMessage(List("www.google.com"))


    "not add invalid URLs to the crawled list" in :
      val probe = testKit.createTestProbe[List[String]]()
      val coordinator = testKit.spawn(Coordinator())
      coordinator ! SetCrawledPages(List("invalid_url"))
      coordinator ! GetCrawledPages(probe.ref)
      probe.expectMessage(List.empty)


    "normalize URLs before adding to the crawled list" in :
      val probe = testKit.createTestProbe[List[String]]()
      val coordinator = testKit.spawn(Coordinator())
      coordinator ! SetCrawledPages(List("http://www.google.com"))
      coordinator ! GetCrawledPages(probe.ref)
      probe.expectMessage(List("www.google.com"))

