package org.unibo.scooby
package coordinator

import io.cucumber.scala.{EN, ScalaDsl}
import org.junit.Assert._
import coordinator.DummyCoordinator.Coordinator

class StepDefinitions extends ScalaDsl with EN:

  private val coordinator = Coordinator()
  private var pages: List[String] = List()
  private var crawablePages: Map[String, Boolean] = Map()

  Given("""I have a list of (.*)$""") { (pages: String) =>
    this.pages = pages.split(",").toList
  }

  When("""Crawler requests the ability to scrape pages""") { () =>
    this.crawablePages = coordinator.checkPages(this.pages)
  }

  Then("""They should return a map of boolean""") { () =>
    assertTrue(this.crawablePages.nonEmpty)
  }



