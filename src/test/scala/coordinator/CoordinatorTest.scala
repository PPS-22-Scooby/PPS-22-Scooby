package org.unibo.scooby
package coordinator

import core.coordinator.Coordinator
import core.coordinator.CoordinatorCommand._

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class CoordinatorTest extends AnyWordSpecLike with BeforeAndAfterAll :

  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "A Coordinator" must :

    "return a map of pages and their crawled status" in :
      val probe = testKit.createTestProbe[PagesChecked]()
      val coordinator = testKit.spawn(Coordinator())
      coordinator ! CheckPages(List("http://www.google.com", "http://www.github.com"), probe.ref)
      probe.expectMessage(PagesChecked(Map("http://www.google.com" -> false, "http://www.github.com" -> false)))


    "return an empty map when no pages are provided" in :
      val probe = testKit.createTestProbe[PagesChecked]()
      val coordinator = testKit.spawn(Coordinator())
      coordinator ! CheckPages(List.empty, probe.ref)
      probe.expectMessage(PagesChecked(Map.empty))


    "return a map with false values for pages that have not been crawled" in :
      val probe = testKit.createTestProbe[PagesChecked]()
      val coordinator = testKit.spawn(Coordinator())
      coordinator ! CheckPages(List("http://www.google.com", "http://www.github.com"), probe.ref)
      probe.expectMessage(PagesChecked(Map("http://www.google.com" -> false, "http://www.github.com" -> false)))


    "return a map with true values for pages that have been crawled" in :
      val probe = testKit.createTestProbe[PagesChecked]()
      val coordinator = testKit.spawn(Coordinator())
      coordinator ! SetCrawledPages(List("http://www.google.com"))
      coordinator ! CheckPages(List("https://www.google.com", "http://www.github.com"), probe.ref)
      probe.expectMessage(PagesChecked(Map("https://www.google.com" -> true, "http://www.github.com" -> false)))


    "return a map with true values for pages that have been checked" in :
      val probe = testKit.createTestProbe[PagesChecked]()
      val coordinator = testKit.spawn(Coordinator())
      coordinator ! CheckPages(List("http://www.google.com"), probe.ref)
      probe.expectMessage(PagesChecked(Map("http://www.google.com" -> false)))
      coordinator ! CheckPages(List("https://www.google.com", "http://www.github.com"), probe.ref)
      probe.expectMessage(PagesChecked(Map("https://www.google.com" -> true, "http://www.github.com" -> false)))


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
