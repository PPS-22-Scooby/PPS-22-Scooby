package org.unibo.scooby
package core.scooby

import core.crawler.ExplorationPolicy
import core.exporter.{AggregationBehavior, ExportingBehavior}
import core.scooby.Configuration.{CrawlerConfiguration, ExporterConfiguration, ScraperConfiguration}
import core.scraper.ScraperPolicies.ScraperPolicy
import utility.document.Document
import utility.http.{ClientConfiguration, URL}
import org.unibo.scooby.core.scooby.Configuration.CoordinatorConfiguration

/**
 * Single Exporting option. Configures one single Exporter
 * @param behavior behavior to be assign to this Exporter
 * @tparam A type inside of [[Result]] that will be exported
 */
enum SingleExporting[A](behavior: ExportingBehavior[A]):
  /**
   * Exporting via stream strategy, meaning that Results will be exported as they are obtained by the scrapers
   */
  case StreamExporting[T](behavior: ExportingBehavior[T]) extends SingleExporting[T](behavior)
  /**
   * Exporting via batch strategy, meaning that Results will be exported <b>only</b> when the whole process has finished
   * It requires also a [[AggregationBehavior]] that specifies how to aggregate previous results with the ones already
   * accumulated.
   */
  case BatchExporting[T](behavior: ExportingBehavior[T],
                         aggregation: AggregationBehavior[T]) extends SingleExporting[T](behavior)

/**
 * Class that wraps all the necessary information to launch a Scooby application.
 * @param crawlerConfiguration configuration for the crawler
 * @param scraperConfiguration configuration for the scraper
 * @param exporterConfiguration configuration for the exporter
 * @tparam T type of [[Result]] that will be exported
 */
case class Configuration[T](crawlerConfiguration: CrawlerConfiguration,
                                           scraperConfiguration: ScraperConfiguration[T],
                                           exporterConfiguration: ExporterConfiguration[T],
                                           coordinatorConfiguration: CoordinatorConfiguration)

object Configuration:
  /**
   * Configuration class for the Crawler.
   * @param url seed URL for the crawling
   * @param explorationPolicy policy that specifies what links to explore inside a [[CrawlDocument]]
   * @param maxDepth max recursion depth for Crawlers. If 0, this results in a simple scraping of the seed URL
   * @param networkOptions additional options on the network side
   */
  case class CrawlerConfiguration(url: URL,
                                  explorationPolicy: ExplorationPolicy,
                                  maxDepth: Int,
                                  networkOptions: ClientConfiguration)

  object CrawlerConfiguration:

    /**
     * Represent the default configuration of the crawler.
     * @return
     */
    def empty: CrawlerConfiguration =
      CrawlerConfiguration(
        URL.empty,
        _ => Iterable.empty, +
          0,
        ClientConfiguration.default
      )


  /**
   * Configuration class for the Scraper
   * @param scrapePolicy policy that specifies what to scrape inside a Document
   * @tparam T type of [[Result]] that will be exported
   */
  case class ScraperConfiguration[T](scrapePolicy: ScraperPolicy[T])

  object ScraperConfiguration:
    def empty[T]: ScraperConfiguration[T] = ScraperConfiguration(_ => Iterable.empty)

  /**
   * Configuration class for the exporter
   * @param exportingStrategies function that consumes (exports) [[Result]]. Inside [[ExportingBehaviors]] several
   *                            utility strategies can be found
   * @tparam T type of the [[Result]] to be exported
   */
  case class ExporterConfiguration[T](exportingStrategies: Seq[SingleExporting[T]])

  object ExporterConfiguration:
    def empty[T]: ExporterConfiguration[T] = ExporterConfiguration(Seq.empty[SingleExporting[T]])


  case class CoordinatorConfiguration(maxLinks: Int)

  def empty[T]: Configuration[T] = Configuration(CrawlerConfiguration.empty, ScraperConfiguration.empty[T],
    ExporterConfiguration.empty[T], CoordinatorConfiguration(100))
