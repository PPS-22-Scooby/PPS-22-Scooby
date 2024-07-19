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

  case class PartialScrape[T]():
    infix def |(context: ExportContext[T]): PartialScrape[T] = this

  def scrape[T](init: ScrapeDocument ?=> Iterable[T])(using builder: ConfigurationBuilder[T]): PartialScrape[T] =
    builder.configuration = builder.configuration.focus(_.scraperConfiguration.scrapePolicy).replace:
      doc =>
        given ScrapeDocument = doc
        init
    PartialScrape[T]()


  def document(using scrapeDocumentContext: ScrapeDocument): ScrapeDocument = scrapeDocumentContext