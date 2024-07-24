package org.unibo.scooby
package dsl
import core.scooby.Configuration
import utility.document.html.HTMLElement
import utility.document.{CommonHTMLExplorer, Document}

import scala.annotation.targetName

object DSL:
  export Config.*
  export Crawl.*
  export Scrape.scrape
  export Export.*
  export Utils.*
  export HTML.*

  case class ScrapingResultSetting[T]()

  class ConfigurationBuilder[T](var configuration: Configuration[T], var scrapingResultSetting: ScrapingResultSetting[T]):
    def build: Configuration[T] = configuration

  private object Utils:

    extension [T](x: Iterable[T])

      /**
       * Applies a map function to an [[Iterable]].
       * @param f the function to apply.
       * @tparam A the return type of the function.
       * @return the [[Iterable]] as result of the mapping function.
       */
      infix def get[A](f: T => A): Iterable[A] = x.map(f)

    extension[T] (x: Iterable[T] )
      infix inline def including[S >: T](y: Iterable[S]): Iterable[S] = x concat y

      infix inline def that(predicate: T => Boolean): Iterable[T] = x filter predicate


    extension[T] (x: T => Boolean)
      infix def and(y: T => Boolean): T => Boolean = el => x(el) && y(el)
      @targetName("and")
      inline infix def &&(y: T => Boolean): T => Boolean = and(y)
      infix def or(y: T => Boolean): T => Boolean = el => x(el) || y(el)
      @targetName("and")
      inline infix def ||(y: T => Boolean): T => Boolean = or(y)
    

