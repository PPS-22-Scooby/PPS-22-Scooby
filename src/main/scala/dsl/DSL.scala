package org.unibo.scooby
package dsl
import core.scooby.Configuration

import scala.compiletime.uninitialized
import org.unibo.scooby.utility.document.{CommonHTMLExplorer, Document}

object DSL:

  export Config.*
  export Crawl.*
  export Scrape.{scrape, matchesOf, select, elements, tag, classes, attributes, get, and, id,
                haveClass, haveId, haveTag, that, dont, including, or, rule}
  export Export.*

  case class ScrapingResultSetting[T]()

  class ConfigurationBuilder[T](var configuration: Configuration[T], var scrapingResultSetting: ScrapingResultSetting[T]):
    def build: Configuration[T] = configuration


  def document[T <: Document & CommonHTMLExplorer](using document: T): T = document
