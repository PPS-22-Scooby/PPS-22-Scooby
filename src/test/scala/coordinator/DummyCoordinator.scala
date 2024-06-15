package org.unibo.scooby
package coordinator

import scala.util.Random

object DummyCoordinator {
  class Coordinator:
    def checkLink(link: String): Boolean =
      Random.nextBoolean()

    def checkPages(pages: List[String]): Map[String, Boolean] =
      pages.map(page => page -> checkLink(page)).toMap
}
