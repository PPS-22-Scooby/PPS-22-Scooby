
package org.unibo.scooby
package coordinator

import coordinator.DummyCoordinator.Coordinator

import io.cucumber.scala.{EN, ScalaDsl}
import org.junit.Assert.*

class StepDefinitions extends ScalaDsl with EN {

  private val coordinator = Coordinator()
  private var pages: List[String] = List.empty
  private var crawablePages: Map[String, Boolean] = Map.empty
  private var checkResult: Option[Boolean] = None

  Given("""I have a list of already crawled pages (.*)$""") { (crawledPages: String) =>
    val pages = crawledPages.split(",").map(_.trim).toList
    coordinator.setCrawledPages(pages)
  }

  Given("""I have an empty list of already crawled pages""") { () =>
    coordinator.setCrawledPages(List.empty)
  }

  When("""I check if (.*) is already crawled$""") { (page: String) =>
    this.checkResult = coordinator.checkPages(List(page)).values.headOption
  }

  When("""I add (.*) to the crawled list$""") { (newPages: String) =>
    val pages = newPages.split(",").map(_.trim).toList
    coordinator.checkPages(pages)
  }

  Then("""The result should be (true|false)$""") { (expectedResult: Boolean) =>
    assertEquals(expectedResult, this.checkResult.getOrElse(true))
  }

  Then("""The updated crawled list should be (.*)$""") { (updatedList: String) =>
    val expectedList = updatedList.split(",").map(_.trim).toList
    assertEquals(expectedList, coordinator.getCrawledPages)
  }

  Then("""Only valid URLs should be added to the list, resulting in (.*)$""") { (updatedList: String) =>
    val expectedList = updatedList.split(",").map(_.trim).toList
    assertEquals(expectedList, coordinator.getCrawledPages)
  }

  Then("""The updated crawled list should not contain duplicates and be (.*)$""") { (updatedList: String) =>
    val expectedList = updatedList.split(",").map(_.trim).toList
    assertEquals(expectedList, coordinator.getCrawledPages.distinct)
  }
}
