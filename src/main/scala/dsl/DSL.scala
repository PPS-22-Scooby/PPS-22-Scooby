package org.unibo.scooby
package dsl
import core.scooby.Configuration

import org.unibo.scooby.utility.document.html.HTMLElement

import scala.compiletime.uninitialized
import org.unibo.scooby.utility.document.{CommonHTMLExplorer, Document}

object DSL:

  export Config.*
  export Crawl.*
  export Scrape.{scrape, matchesOf, select, elements, classes, attributes, and, id,
                haveClass, haveId, haveTag, that, dont, including, or, rule}
  export Export.*
  export Utils.*

  case class ScrapingResultSetting[T]()

  class ConfigurationBuilder[T](var configuration: Configuration[T], var scrapingResultSetting: ScrapingResultSetting[T]):
    def build: Configuration[T] = configuration

  object Utils:

    /**
     * Retrieves tag from an [[HTMLElement]]
     * @return the tag as [[String]].
     */
    def tag: HTMLElement => String = _.tag

    /**
     * Retrieves text from an [[HTMLElement]]
     * @return text as [[String]].
     */
    def text: HTMLElement => String = _.text

    /**
     * Retrieves outer Html text from an [[HTMLElement]]
     * @return outer Html text as [[String]].
     */
    def outerText: HTMLElement => String = _.outerHtml

    /**
     * Retrieves value of the specified attribute of an [[HTMLElement]].
     * @return outer Html text as [[String]].
     */

    /**
     * Retrieves value of the specified attribute of an [[HTMLElement]].
     * @param attribute the attribute to read the value.
     * @return the value of the attribute as [[String]].
     */
    def attr(attribute: String): HTMLElement => String = _.attr(attribute)

    extension [T](x: Iterable[T])

      /**
       * Applies a map function to an [[Iterable]].
       * @param f the function to apply.
       * @tparam A the return type of the function.
       * @return the [[Iterable]] as result of the mapping function.
       */
      infix def get[A](f: T => A): Iterable[A] = x.map(f)
    

  def document[T <: Document & CommonHTMLExplorer](using document: T): T = document
