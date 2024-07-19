package org.unibo.scooby
package dsl

import core.exporter.Exporter.AggregationBehaviors
import core.scooby.SingleExporting
import core.scraper.ScraperPolicies.ScraperPolicy
import dsl.DSL.ConfigurationBuilder
import dsl.Export.ExportContext
import utility.document.ScrapeDocument

import monocle.syntax.all.*

object Scrape:

  def scrape[T](init: ScrapeDocument ?=> Iterable[T])(using builder: ConfigurationBuilder[T]): Unit =
    builder.configuration = builder.configuration.focus(_.scraperConfiguration.scrapePolicy).replace:
      doc =>
        given ScrapeDocument = doc
        init
    builder.exportContext = ExportContext[T]()


  def document(using scrapeDocumentContext: ScrapeDocument): ScrapeDocument = scrapeDocumentContext