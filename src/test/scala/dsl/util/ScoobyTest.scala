package org.unibo.scooby
package dsl.util

import core.scooby
import core.scooby.{Configuration, SingleExporting}
import core.scraper.Result
import dsl.DSL.ConfigurationBuilder
import dsl.ScoobyEmbeddable
import utility.document.{CrawlDocument, ScrapeDocument}
import utility.http.Clients.SimpleHttpClient
import utility.http.api.Calls.GET
import utility.http.{ClientConfiguration, HttpError, URL}
import utility.{MockServer, ScalaTestWithMockServer}

import akka.http.scaladsl.server.Route
import org.scalatest.BeforeAndAfterEach
import org.unibo.scooby.core.scraper.ScraperPolicies.ScraperPolicy

import java.io.{ByteArrayOutputStream, PrintStream}
import java.nio.file.{Files, Path}
import scala.compiletime.uninitialized
import scala.util.matching.Regex

class ScoobyTest(mockServerPort: Int = 8080, route: Route = MockServer.Routes.staticHtmlRoutes)
  extends ScoobyEmbeddable with ScalaTestWithMockServer(port = mockServerPort, routes = route)
  with BeforeAndAfterEach:

  given SimpleHttpClient = SimpleHttpClient()

  var path: Path = uninitialized
  val baseURL: URL = URL("http://localhost:" + 8080)
  
  given Conversion[URL, String] = url => url.toString

  override def beforeEach(): Unit =
    path = Files.createTempDirectory("scooby-test")
    path.toFile.deleteOnExit()

  override def afterEach(): Unit =
    Files.walk(path)
      .sorted(java.util.Comparator.reverseOrder())
      .forEach(Files.deleteIfExists(_))

  def mockedScooby[T](init: ConfigurationBuilder[T] ?=> Unit): Configuration[T] =
    scooby(init).config

  extension[T] (config: Configuration[T])

    def assertSame(networkOptions: ClientConfiguration): Configuration[T] =
      config.crawlerConfiguration.networkOptions shouldBe networkOptions
      config

    def assertSame(url: URL): Configuration[T] =
      config.crawlerConfiguration.url shouldBe url
      config

    def assertSame(maxDepth: Int, maxLinks: Int): Configuration[T] =
      config.crawlerConfiguration.maxDepth shouldBe maxDepth
      config.coordinatorConfiguration.maxLinks shouldBe maxLinks
      config

    def mockScraping(doc: ScrapeDocument): Iterable[T] =
      config.scraperConfiguration.scrapePolicy(doc)

    def mockCrawling(doc: CrawlDocument): Iterable[URL] =
      config.crawlerConfiguration.explorationPolicy(doc)

    def mockExporting(results: Result[T]): Unit =
      config.exporterConfiguration.exportingStrategies.foreach:
        case SingleExporting.StreamExporting(behavior) => behavior(results)
        case SingleExporting.BatchExporting(behavior, _) => behavior(results)

    def expectResultFromScraping(serverURL: URL, results: Iterable[T]): Unit =
      val docEither: Either[HttpError, ScrapeDocument] = GET(serverURL)
      docEither.isRight shouldBe true
      mockScraping(docEither.getOrElse(fail())).toList should contain theSameElementsAs results

    def expectCrawledLinks(links: URL*): Unit =
      val docEither: Either[HttpError, CrawlDocument] = GET(config.crawlerConfiguration.url)
      docEither.isRight shouldBe true
      mockCrawling(docEither.getOrElse(fail())) shouldBe links

    def scrapeExportInspectFileContains(serverURL: URL, filePath: Path, expected: String,
                                        scraping: ScraperPolicy[T] = _.getAllElements): Unit =
      val docEither: Either[HttpError, ScrapeDocument] = GET(serverURL)
      docEither.isRight shouldBe true
      val results = scraping(docEither.getOrElse(fail()))
      mockExporting(Result(results))
      Files.exists(filePath) shouldBe true
      Files.readString(filePath) shouldBe expected

    def scrapeExportInspectConsoleContains(serverURL: URL, expected: Iterable[String], separator: String,
                                           scraping: ScraperPolicy[T] = _.getAllElements): Unit =
      
      val docEither: Either[HttpError, ScrapeDocument] = GET(serverURL)
      docEither.isRight shouldBe true
      val results = scraping(docEither.getOrElse(fail()))

      val outCapture = new ByteArrayOutputStream()
      Console.withOut(new PrintStream(outCapture)) {
        mockExporting(Result(results))
      }

      def countOccurrences(mainStr: String, subStr: String): Int = {
        val regex: Regex = subStr.r
        regex.findAllMatchIn(mainStr).length
      }
      
      val listOccurrences = expected.groupBy(identity).view.mapValues(_.size)
      
      val allElementsMatchCount = listOccurrences.forall { case (element, count) =>
        countOccurrences(outCapture.toString, element) >= count
      }

      allElementsMatchCount shouldBe true



