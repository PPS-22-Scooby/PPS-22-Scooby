package org.unibo.scooby
package dsl
import core.scooby.Configuration

import org.unibo.scooby.utility.document.html.HTMLElement

import scala.compiletime.uninitialized

object DSL:

  export Config.*
  export Crawl.*
  export Scrape.{scrape, document, matchesOf, select, elements, classes, attributes, and, id,
                haveClass, haveId, haveTag, that, dont, including, or}
  export Export.*
  export Utils.*

  case class ScrapingResultSetting[T]()

  class ConfigurationBuilder[T](var configuration: Configuration[T], var scrapingResultSetting: ScrapingResultSetting[T]):
    def build: Configuration[T] = configuration

  object Utils:
    def tag: HTMLElement => String = _.tag

    extension [T](x: Iterable[T])
      infix def get[A](f: T => A): Iterable[A] = x.map(f)
    