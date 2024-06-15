package org.unibo.scooby
package scraper

object DummyScraper:

  class Scraper(regEx: String => Boolean):

    // Suppose a regEx.yaml like
    // regex:
    //   - HTMLElem:
    //      textRegex: "^p.*x$".r
    //      attrRegex: attr => elem.has(attr) and attr == myAttr

    def scrape(page: String): String =

      // Parse the HTML
      val filteredPage = if regEx.apply(page) then page else "Nothing found"

      filteredPage

