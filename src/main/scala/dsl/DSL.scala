package org.unibo.scooby
package dsl
import core.scooby.Configuration

import scala.compiletime.uninitialized

object DSL:

  export Config.*
  export Crawl.*
  export Scrape.{scrape, document, matchesOf, select, elements, tag, classes, attributes, get, and, id,
                haveClass, haveId, haveTag, that, dont, including, or, haveAttribute, haveAttributeValue}
  export Export.*

  case class ScrapingResultSetting[T]()

  class ConfigurationBuilder[T](var configuration: Configuration[T], var scrapingResultSetting: ScrapingResultSetting[T]):
    def build: Configuration[T] = configuration

    