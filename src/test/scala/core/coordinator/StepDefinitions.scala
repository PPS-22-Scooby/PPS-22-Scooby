package org.unibo.scooby
package core.coordinator

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import core.coordinator.Coordinator
import core.coordinator.CoordinatorCommand.*

import io.cucumber.scala.{EN, ScalaDsl}
import org.junit.Assert.*
import core.crawler.CrawlerCommand.CrawlerCoordinatorResponse

class StepDefinitions extends ScalaDsl with EN :

  private val testKit = ActorTestKit()
  private val coordinator = testKit.spawn(Coordinator())
  private var pages: List[String] = List.empty
  private var crawablePages: Map[String, Boolean] = Map.empty
  private var checkResult: Option[Boolean] = None

  Given("""I have a list of already crawled pages (.*)$""") :
    (crawledPages: String) =>
      val pages = crawledPages.split(",").map(_.trim).toList
      coordinator ! SetCrawledPages(pages)


  Given("""I have an empty list of already crawled pages""") :
    () => coordinator ! SetCrawledPages(List.empty)


  When("""I check if (.*) is crawable$""") :
    (page: String) =>
      val probe = testKit.createTestProbe[CrawlerCoordinatorResponse]()
      coordinator ! CheckPages(List(page), probe.ref)
      this.checkResult = Some(probe.receiveMessage().result.hasNext)


  When("""I add (.*) to the crawled list$""") :
    (newPages: String) =>
      val pages = newPages.split(",").map(_.trim).toList
      coordinator ! SetCrawledPages(pages)


  Then("""The result of check should be (true|false)$""") :
    (expectedResult: Boolean) =>
      assertEquals(expectedResult, this.checkResult.getOrElse(true))


  Then("""The updated crawled list should be (.*)$""") :
    (updatedList: String) =>
      val probe = testKit.createTestProbe[List[String]]()
      coordinator ! GetCrawledPages(probe.ref)
      val expectedList = updatedList.split(",").map(_.trim).toList
      assertEquals(expectedList, probe.receiveMessage())


  Then("""Only valid URLs should be added to the list, resulting in (.*)$""") :
    (updatedList: String) =>
      val probe = testKit.createTestProbe[List[String]]()
      coordinator ! GetCrawledPages(probe.ref)
      val expectedList = updatedList.split(",").map(_.trim).toList
      assertEquals(expectedList, probe.receiveMessage())


  Then("""The updated crawled list should not contain duplicates and be (.*)$"""):
    (updatedList: String) =>
      val probe = testKit.createTestProbe[List[String]]()
      coordinator ! GetCrawledPages(probe.ref)
      val expectedList = updatedList.split(",").map(_.trim).toList
      assertEquals(expectedList, probe.receiveMessage().distinct)

