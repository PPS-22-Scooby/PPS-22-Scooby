package org.unibo.scooby
package dsl

import core.exporter.Exporter.AggregationBehaviors
import core.scooby.SingleExporting
import core.scraper.ScraperPolicies.ScraperPolicy
import dsl.DSL.ConfigurationBuilder
import utility.document.ScrapeDocument

import monocle.syntax.all.*
import org.unibo.scooby.dsl.DSL.ScrapingResultSetting

object Scrape:

  def scrape[T](init: ScrapeDocument ?=> Iterable[T])(using builder: ConfigurationBuilder[T]): Unit =
    builder.configuration = builder.configuration.focus(_.scraperConfiguration.scrapePolicy).replace:
      doc =>
        given ScrapeDocument = doc
        init
    builder.scrapingResultSetting = ScrapingResultSetting[T]()


  def document(using scrapeDocumentContext: ScrapeDocument): ScrapeDocument = scrapeDocumentContext