package org.unibo.scooby
package dsl
import core.scooby.Configuration
import utility.document.html.HTMLElement
import utility.document.{CommonHTMLExplorer, Document}

import scala.annotation.targetName

/**
 * Main entry point of the DSL keywords. Importing `DSL.*` will make all the keywords of the language available.
 */
object DSL:
  export Config.*
  export Crawl.*
  export Scrape.scrape
  export Export.*
  export Utils.*
  export HTML.*

  /**
   * Temporary empty class to hold the scraping result type.
   * @tparam T type of the scraping result
   */
  case class ScrapingResultSetting[T]()

  /**
   * Builder available at the global level of the DSL, necessary to build the final Scooby configuration
   * @param configuration variable configuration, modified throughout the DSL usage
   * @param scrapingResultSetting object symbolizing the scraping result type
   * @tparam T type of the [[Configuration]]
   */
  class ConfigurationBuilder[T](var configuration: Configuration[T], var scrapingResultSetting: ScrapingResultSetting[T]):
    def build: Configuration[T] = configuration

  /**
   * Container for several utility keywords of the DSL
   */
  private object Utils:

    extension [T](x: Iterable[T])

      /**
       * Applies a map function to an [[Iterable]].
       * @param f the function to apply.
       * @tparam A the return type of the function.
       * @return the [[Iterable]] as result of the mapping function.
       */
      inline infix def get[A](f: T => A): Iterable[A] = x.map(f)
      /**
       * Adds another iterable to this iterable.
       * @param toInclude another [[Iterable]] to include (i.g. the one added)
       * @tparam S type of the second iterable
       * @return another [[Iterable]] resulted from the concatenation
       */
      inline infix def including[S >: T](toInclude: Iterable[S]): Iterable[S] = x concat toInclude
      /**
       * Applies a filter function to an [[Iterable]]
       * @return a filtered iterable
       */
      inline infix def that(predicate: T => Boolean): Iterable[T] = x filter predicate

    /**
     * Return the predicate negated.
     * @param predicate predicate to be negated
     * @tparam T type of the element fed to the predicate
     * @return the negation of the predicate
     */
    inline infix def dont[T](predicate: T => Boolean): T => Boolean = elem => !predicate(elem)


    extension[T] (x: T => Boolean)
      /**
       * Intersection of two predicates
       * @param y the other predicate
       * @return a predicate returning true when both the previous predicates return true
       */
      inline infix def and(y: T => Boolean): T => Boolean = el => x(el) && y(el)
      /**
       * Alias for [[and]].
       * @param y the other predicate
       * @return a predicate returning true when both the previous predicates return true
       */
      @targetName("and")
      inline infix def &&(y: T => Boolean): T => Boolean = and(y)
      /**
       * Union of two predicates.
       * @param y the other predicate
       * @return a predicate returning true when at least one of the previous predicates return true
       */
      inline infix def or(y: T => Boolean): T => Boolean = el => x(el) || y(el)
      /**
       * Alias for [[or]]
       * @param y the other predicate
       *          * @return a predicate returning true when at least one of the previous predicates return true
       */
      @targetName("and")
      inline infix def ||(y: T => Boolean): T => Boolean = or(y)
    

