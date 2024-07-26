package org.unibo.scooby
package dsl

import dsl.util.ScoobyTest
import utility.document.html.HTMLElement
import utility.http.URL.url

import utility.document.{CrawlDocument, ScrapeDocument}
import utility.http.{HttpError, URL}
import utility.http.api.Calls.GET


class DSLScraperTest extends ScoobyTest:

  private def getScrapeDocument(url: URL): ScrapeDocument =
    val exampleDom: Either[HttpError, CrawlDocument] = GET(url)
    val document = exampleDom match
      case Left(e) =>
        ScrapeDocument("", url)
      case Right(document) =>
        ScrapeDocument(document.content, document.url)
    document

  "Scrape" should "return all html if no condition is specified" in:
    val targetUrl = url"http://example.com"
    val scrapedDocument = getScrapeDocument(targetUrl)
    mockedScooby:
      crawl:
        url:
          targetUrl
        policy:
          hyperlinks
        scrape:
          elements
    .expectResultFromScraping(targetUrl, scrapedDocument.getAllElements)

  it should "return no html at all if no scrape policy is present" in:
    mockedScooby:
      crawl:
        url:
          url"http://example.com"
        policy:
          hyperlinks
    .expectResultFromScraping(url"http://example.com", List())


  it should "be able to filter html elements by tag" in:
    val targetUrl = url"http://example.com"
    val scrapedDocument = getScrapeDocument(targetUrl)
    mockedScooby:
      crawl:
        url:
          targetUrl
        policy:
          hyperlinks
        scrape:
          elements that:
            haveTag("p")
    .expectResultFromScraping(url"http://example.com", scrapedDocument.getElementsByTag("p"))

  it should "be able to filter html elements by class" in:
    val scrapeDocument = getScrapeDocument(baseURL / "level1.0.html")
    mockedScooby:
      crawl:
        url:
          baseURL / "level1.0.html"
        policy:
          hyperlinks
        scrape:
          elements that :
            haveClass("lorem")
    .expectResultFromScraping(baseURL / "level1.0.html", scrapeDocument.getElementsByClass("lorem"))


  it should "be able to filter html elements by attribute value" in :
    val scrapeDocument = getScrapeDocument(baseURL / "level1.0.html")
    mockedScooby:
      crawl:
        url:
          baseURL / "level1.0.html"
        policy:
          hyperlinks
        scrape:
          elements that :
            haveAttributeValue("class", "lorem")
    .expectResultFromScraping(baseURL / "level1.0.html", scrapeDocument.getElementsByClass("lorem"))

  it should "be able to concatenate two conditions using or" in:
    val scrapeDocument = getScrapeDocument(baseURL / "level2.0.html")
    val expectedResult = scrapeDocument.select("a[href^=\"level\"]")
    mockedScooby:
      crawl:
        url:
          baseURL / "level2.0.html"
        policy:
          hyperlinks
        scrape:
          elements that :
            haveAttributeValue("href", "level1.0.html") or haveAttributeValue("href", "level1.1.html")
    .expectResultFromScraping(baseURL / "level2.0.html", expectedResult)

  it should "be able to concatenate two conditions using and" in:
    val scrapeDocument = getScrapeDocument(baseURL / "level2.0.html")
    val expectedResult = scrapeDocument.select("a[href^=\"level1.1\"]")

    mockedScooby:
      crawl:
        url:
          baseURL / "level2.0.html"
        policy:
          hyperlinks
        scrape:
          elements that :
            haveAttributeValue("href", "level1.1.html") and haveClass("amet")
    .expectResultFromScraping(baseURL / "level2.0.html", expectedResult)


