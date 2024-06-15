package org.unibo.scooby
package coordinator

import scala.util.Random

object DummyCoordinator {
  class Coordinator:

    private var crawledPages: List[String] = List.empty

    /**
     * Checks whether the provided pages have already been crawled and updates the list of crawled pages.
     *
     * @param pages The list of pages to be verified.
     * @return A map that associates each page with a boolean value indicating whether the page has already been crawled.
     */
    def checkPages(pages: List[String]): Map[String, Boolean] =
      pages.map { page =>
        val isCrawled = crawledPages.contains(page)
        if (!isCrawled) {
          crawledPages = crawledPages.appended(page)
        }
        page -> isCrawled
      }.toMap
      
    def getCrawledPages: List[String] = crawledPages
}
