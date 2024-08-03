package org.unibo.scooby
package dsl

import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.{be, should}
import org.unibo.scooby.core.crawler.ExplorationPolicies
import org.unibo.scooby.core.scooby.Configuration.{CoordinatorConfiguration, CrawlerConfiguration, ExporterConfiguration, ScraperConfiguration}
import org.unibo.scooby.core.scooby.SingleExporting.BatchExporting
import org.unibo.scooby.core.scooby.Configuration
import org.unibo.scooby.core.scraper.Result
import org.unibo.scooby.utility.document.html.HTMLElement
import org.unibo.scooby.utility.document.{CommonHTMLExplorer, CrawlDocument, Document, ScrapeDocument}
import org.unibo.scooby.utility.http.{ClientConfiguration, URL}

import java.nio.file.{Files, Path}
import scala.compiletime.uninitialized
import scala.concurrent.Await
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class DSLTest extends AnyFlatSpec, ScoobyEmbeddable, BeforeAndAfterEach, Matchers:

  var path: Path = uninitialized

  val tout: FiniteDuration = 9.seconds
  val maxRequest: Int = 5
  val auth: (String, String) = ("Authorization", "prova")
  // val agent: (String, String) = ("Agent", "gr")
  val maxDepth: Int = 2
  val maxLinks: Int = 20
  val url: String = "https://www.example.com"
  val directory: String = "DSL-application-test"
  val fileDSL: String = "test-DSL.txt"
  var filePathDSL: Path = uninitialized
  val fileStandard: String = "test-standard.txt"
  var filePathStandard: Path = uninitialized

  def linksPolicy(using CrawlDocument): Iterable[URL] = hyperlinks

  def scrapeToIter(doc: ScrapeDocument): Iterable[HTMLElement] =
    doc.getAllElements

  def scrapePolicy[T](policy: ScrapeDocument => Iterable[T]): ScrapeDocument => Iterable[T] =
    doc => policy(doc)

  def scrapePolicyDSL[D <: Document & CommonHTMLExplorer, T](toIter: D => Iterable[T]): D ?=> Iterable[T] =
    toIter(document)

  def resultsToCheckAsMapWithSize[T1, T2](res: Result[T1], f: T1 => T2): Map[T2, Int] =
    res.data.groupMapReduce(f)(_ => 1)(_ + _)

  def batchStrategy(file: String): Iterable[HTMLElement] => Unit = it =>
    it.groupMapReduce(_.tag)(_ => 1)(_ + _) output:
      toFile(file) withFormat text

  def batchStrategyDSL(file: String): Iterable[HTMLElement] ?=> Unit =
    batchStrategy(file)(results)

  def batchAggregation[T]: (Iterable[T], Iterable[T]) => Iterable[T] = _ ++ _

  def streamingStrategyDSL: Iterable[HTMLElement] ?=> Unit =
    results.groupBy(_.tag).view.mapValues(_.size).toMap output:
      toConsole withFormat text

  override def beforeEach(): Unit =
    path = Files.createTempDirectory(directory)
    path.toFile.deleteOnExit()
    filePathDSL = path.resolve(fileDSL)
    filePathStandard = path.resolve(fileStandard)

  override def afterEach(): Unit =
    Files.walk(path)
      .sorted(java.util.Comparator.reverseOrder())
      .forEach(Files.deleteIfExists(_))

  "Application with DSL configuration and standard configurations" should "obtain the same result" in :

    val appDSL = scooby:
      config:
        network:
          Timeout is tout
          MaxRequests is maxRequest
          headers:
            auth._1 to auth._2
        options:
          MaxDepth is maxDepth
          MaxLinks is maxLinks

      crawl:
        url:
          this.url
        policy:
          linksPolicy
      scrape:
        scrapePolicyDSL(scrapeToIter)
      exports:
        batch:
          strategy:
            batchStrategyDSL(filePathDSL.toString)
          aggregate:
            batchAggregation

    val appStandard = ScoobyRunnable(
      Configuration(
        CrawlerConfiguration(
          URL(url),
          ExplorationPolicies.allLinks,
          maxDepth,
          ClientConfiguration(tout, maxRequest, Map(auth))
        ),
        ScraperConfiguration(scrapeToIter),
        ExporterConfiguration(Seq(
          BatchExporting(
            (res: Result[HTMLElement]) => batchStrategy(filePathStandard.toString)(res.data),
            (res1: Result[HTMLElement], res2: Result[HTMLElement])=> Result(batchAggregation(res1.data, res2.data))
          ))),
        CoordinatorConfiguration(maxLinks)
      )
    )

    val resultDSL: Map[String, Int] = resultsToCheckAsMapWithSize(Await.result(appDSL.run(), 10.seconds), _.tag)
    val resultStandard: Map[String, Int] = resultsToCheckAsMapWithSize(Await.result(appStandard.run(), 10.seconds), _.tag)

    resultDSL shouldBe resultStandard
