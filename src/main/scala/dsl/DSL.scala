package org.unibo.scooby
package dsl
import core.scooby.Configuration

import scala.compiletime.uninitialized
import org.unibo.scooby.utility.document.Document

object DSL:

  export Config.*
  export Crawl.*
  export Scrape.*
  export Export.*

  case class ScrapingResultSetting[T]()

  class ConfigurationBuilder[T](var configuration: Configuration[T], var scrapingResultSetting: ScrapingResultSetting[T]):
    def build: Configuration[T] = configuration


  def document[T <: Document](using document: T): T = document
