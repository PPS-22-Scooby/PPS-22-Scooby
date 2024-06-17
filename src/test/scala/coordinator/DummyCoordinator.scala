package org.unibo.scooby
package coordinator

import scala.util.Random

object DummyCoordinator :
  class Coordinator :

    private var crawledPages: Set[String] = Set.empty

    /**
     * Checks whether the provided pages have already been crawled and updates the list of crawled pages.
     *
     * @param pages The list of pages to be verified.
     * @return A map that associates each page with a boolean value indicating whether the page has already been crawled.
     */
    def checkPages(pages: List[String]): Map[String, Boolean] = 
      pages.map : page =>
        val normalizedPage = normalizeURL(page)
        val isCrawled = crawledPages.contains(normalizedPage)
        if (isValidURL(page) && !isCrawled) {
          crawledPages += normalizedPage
        }
        page -> isCrawled
      .toMap

    /**
     * Sets the list of already crawled pages.
     *
     * @param pages The list of pages to set as already crawled.
     */
    def setCrawledPages(pages: List[String]): Unit = crawledPages = pages.filter(isValidURL).map(normalizeURL).toSet

    /**
     * Gets the list of already crawled pages.
     *
     * @return The list of already crawled pages.
     */
    def getCrawledPages: List[String] = crawledPages.toList

    /**
     * Normalizes the URL by removing the protocol for comparison purposes but keeping the protocol in the stored set.
     *
     * @param url The URL to be normalized.
     * @return The normalized URL.
     */
    private def normalizeURL(url: String): String = url.replaceFirst("^(http://|https://)", "")

    /**
     * Validates if the provided URL is valid.
     *
     * @param url The URL to be validated.
     * @return True if the URL is valid, otherwise false.
     */
    private def isValidURL(url: String): Boolean = 
      val regex = "^(http://|https://)[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,}(\\S*)?$"
      url.matches(regex)

