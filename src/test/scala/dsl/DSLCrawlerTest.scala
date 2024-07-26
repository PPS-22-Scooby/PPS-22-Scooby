package org.unibo.scooby
package dsl

import dsl.util.ScoobyTest
import utility.http.URL.toUrl

class DSLCrawlerTest extends ScoobyTest:

  "Crawl" should "return all hyperlinks in the html" in :
    mockedScooby:
      crawl:
        url:
          baseURL / "level1.0.html"
        policy:
          hyperlinks
    .expectCrawledLinks(
      baseURL / "level2.0.html",
      baseURL / "level2.1.html",
      baseURL / "index.html",
      "http://example.com".toUrl
    )

  "Crawl" should "return all links in the html" in :
    mockedScooby:
      crawl:
        url:
          baseURL / "level1.0.html"
        policy:
          allLinks
    .expectCrawledLinks(
      baseURL / "style.css",
      baseURL / "level2.0.html",
      baseURL / "level2.1.html",
      baseURL / "index.html",
      "http://example.com".toUrl
    )

  "Crawl" should "return all, not external, links in the html" in :
    mockedScooby:
      crawl:
        url:
          baseURL / "level1.0.html"
        policy:
          allLinks not external
    .expectCrawledLinks(
      baseURL / "style.css",
      baseURL / "level2.0.html",
      baseURL / "level2.1.html",
      baseURL / "index.html"
    )

  "Crawl" should "return all, not external, hyperlinks in the html" in :
    mockedScooby:
      crawl:
        url:
          baseURL / "level1.0.html"
        policy:
          hyperlinks not external
    .expectCrawledLinks(
      baseURL / "level2.0.html",
      baseURL / "level2.1.html",
      baseURL / "index.html"
    )

