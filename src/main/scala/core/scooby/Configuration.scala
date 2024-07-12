package org.unibo.scooby
package core.scooby

import core.crawler.ExplorationPolicy
import core.exporter.{AggregationBehavior, ExportingBehavior}
import core.scooby.Configuration.{CrawlerConfiguration, ExporterConfiguration, ScraperConfiguration}
import core.scraper.ScraperPolicy
import utility.document.Document
import utility.http.Configuration.ClientConfiguration
import org.unibo.scooby.utility.http.URL

enum SingleExporting[A](behavior: ExportingBehavior[A]):
  case StreamExporting[T](behavior: ExportingBehavior[T]) extends SingleExporting[T](behavior)
  case BatchExporting[T](behavior: ExportingBehavior[T],
                         aggregation: AggregationBehavior[T]) extends SingleExporting[T](behavior)

case class Configuration[D <: Document, T](crawlerConfiguration: CrawlerConfiguration,
                                           scraperConfiguration: ScraperConfiguration[D, T],
                                           exporterConfiguration: ExporterConfiguration[T])

object Configuration:
  case class CrawlerConfiguration(url: URL,
                                  explorationPolicy: ExplorationPolicy,
                                  maxDepth: Int,
                                  networkOptions: ClientConfiguration)

  case class ScraperConfiguration[D <: Document, T](scrapePolicy: ScraperPolicy[D, T])

  case class ExporterConfiguration[T](exportingStrategies: Seq[SingleExporting[T]])



