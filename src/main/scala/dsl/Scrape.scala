package org.unibo.scooby
package dsl

import dsl.DSL.{ConfigurationBuilder, ScrapingResultSetting}
import utility.document.*

import monocle.syntax.all.*

/**
 * Container for all the scraping-related DSL keywords.
 */
object Scrape:
  export SafeOps.*
  /**
   * Type alias representing the "scrape" section of the DSL
   * @tparam T type of the results obtained doing by this scraping
   */
  private type ScrapeBehaviorScope[T] = ScrapeDocument ?=> Iterable[T]
  /**
   * Facade for Scraping DSL operators that performs syntax checks.
   */
  object SafeOps:
    import UnsafeOps.*
    import dsl.syntax.catchRecursiveCtx

    /**
     * Top level keyword for defining the scraping behavior.
     * @param block definition of the scraping behavior
     * @param globalScope global Scooby scope (i.g. "scooby: ...")
     * @tparam T type of the result returned by this scraping behavior
     */
    inline def scrape[T](block: ScrapeBehaviorScope[T])(using globalScope: ConfigurationBuilder[T]): Unit =
      catchRecursiveCtx[ScrapeDocument]("scrape")
      scrapeOp(block)

  /**
   * Private collection of unsafe operators. The related safe versions are contained inside [[SafeOps]]
   */
  private[Scrape] object UnsafeOps:
    /**
     * Unsafe version of the one inside [[SafeOps]]
     * @param block definition of the scraping behavior
     * @param globalScope global Scooby scope (i.g. "scooby: ...")
     * @tparam T type of the result returned by this scraping behavior
     */
    def scrapeOp[T](block: ScrapeBehaviorScope[T])(using globalScope: ConfigurationBuilder[T]): Unit =
      globalScope.configuration = globalScope.configuration.focus(_.scraperConfiguration.scrapePolicy).replace:
        doc =>
          given ScrapeDocument = doc
          block
      globalScope.scrapingResultSetting = ScrapingResultSetting[T]()

