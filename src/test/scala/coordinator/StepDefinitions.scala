package org.unibo.scooby
package coordinator

import io.cucumber.scala.{EN, ScalaDsl}
import org.junit.Assert._
import coordinator.DummyCoordinator.Coordinator

class StepDefinitions extends ScalaDsl with EN:

  private val coordinator = Coordinator()
  private var pages: List[String] = List.empty
  private var crawablePages: Map[String, Boolean] = Map.empty
  private var checkResult: Option[Boolean] = None

  Given("""I have a list of (.*)$""") { (pages: String) =>
    this.pages = pages.split(",").toList
  }

  When("""Crawler requests the ability to scrape pages""") { () =>
    this.crawablePages = coordinator.checkPages(this.pages)
  }

  Then("""They should return a map of boolean""") { () =>
    assertTrue(this.crawablePages.nonEmpty)
  }

  Given("""I have this (.*) are already crawled$""") { (pages: String) =>
    val crawledPages = pages.replaceAll("\\s", "").split(",").toList
    coordinator.checkPages(crawledPages)
  }

  When("""I check if (.*) is already crawled$""") { (page: String) =>
    this.checkResult = coordinator.checkPages(List(page)).values.headOption
  }

  Then("""The result should be (true|false)$""") { (expectedResult: Boolean) =>
    assertEquals(expectedResult, this.checkResult.getOrElse(false))
  }