package org.unibo.scooby
package core.coordinator

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import core.coordinator.CoordinatorCommand.*

import io.cucumber.scala.{EN, ScalaDsl}
import org.junit.Assert.*
import core.crawler.CrawlerCommand.CrawlerCoordinatorResponse
import utility.http.URL.toUrl
import utility.http.URL

class StepDefinitions extends ScalaDsl with EN :

  private val testKit = ActorTestKit()
  private val coordinator = testKit.spawn(Coordinator())
  private var isCrawled: Boolean = false

  Given("""I have a list of already crawled pages (.*)$""") :
    (crawledPages: String) =>
      val pages = crawledPages.split(",").map(_.trim.toUrl).toList
      coordinator ! SetCrawledPages(pages)

  Given("""I have an empty list of already crawled pages""") :
    () => coordinator ! SetCrawledPages(List.empty[URL])

  When("""I check if (.*) is already crawled$""") :
    (page: String) =>
      val probe = testKit.createTestProbe[CrawlerCoordinatorResponse]()
      val convPage = page.toUrl
      coordinator ! CheckPages(List(convPage), probe.ref)
      val retrievedPage = probe.receiveMessage().result
      this.isCrawled = !retrievedPage.contains(page.toUrl)

  When("""I add (.*) to the crawled list$""") :
    (newPages: String) =>
      val pages = newPages.split(",").map(_.trim.toUrl).toList
      coordinator ! SetCrawledPages(pages)

  Then("""The coordinator response result should be true$""") :
    () => assertTrue(this.isCrawled)

  Then("""The coordinator response result should be false$"""):
    () =>assertFalse(this.isCrawled)

  Then("""The updated crawled list should be (.*)$""") :
    (updatedList: String) =>
      val probe = testKit.createTestProbe[List[URL]]()
      coordinator ! GetCrawledPages(probe.ref)
      val expectedList = updatedList.split(",").map(_.trim.toUrl).toList
      assertEquals(expectedList, probe.receiveMessage())


  Then("""Only valid URLs should be added to the list, resulting in (.*)$""") :
    (updatedList: String) =>
      val probe = testKit.createTestProbe[List[URL]]()
      coordinator ! GetCrawledPages(probe.ref)
      val expectedList = updatedList.split(",").map(_.trim.toUrl).toList
      assertEquals(expectedList, probe.receiveMessage())


  Then("""The updated crawled list should not contain duplicates and be (.*)$"""):
    (updatedList: String) =>
      val probe = testKit.createTestProbe[List[URL]]()
      coordinator ! GetCrawledPages(probe.ref)
      val expectedList = updatedList.split(",").map(_.trim.toUrl).toList
      assertEquals(expectedList, probe.receiveMessage().distinct)
