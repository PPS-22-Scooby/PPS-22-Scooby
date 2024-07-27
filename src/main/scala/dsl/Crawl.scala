package org.unibo.scooby
package dsl

import core.crawler.ExplorationPolicy
import dsl.DSL.ConfigurationBuilder
import utility.document.CrawlDocument
import utility.http.URL

import monocle.syntax.all.*
import dsl.syntax.catchRecursiveCtx

/**
 * Collection of DSL operators to customize the Crawl properties of Scooby application
 *
 * Example usage:
 * {{{
 *   crawl:
 *     url:
 *       "https://www.example.com"
 *     policy:
 *       hyperlinks not external
 * }}}
 */
object Crawl:
  export SafeOps.*

  /**
   * Context under "crawl"
   *
   * @param url    the URL to start crawling from
   * @param policy the exploration policy to use
   */
  case class CrawlContext(var url: String, var policy: ExplorationPolicy)

  /**
   * Type alias representing the section under "crawl"
   */
  private type CrawlScope = CrawlContext ?=> Unit
  /**
   * Type alias representing the section under "policy"
   */
  private type PolicyScope = CrawlDocument ?=> Iterable[URL]

  object SafeOps:
    import UnsafeOps.*

    /**
     * Top level keyword for defining the crawl configuration
     *
     * @param block   nested definitions of crawl settings
     * @param globalScope global scooby scope
     * @tparam T type of the configuration
     */
    inline def crawl[T](block: CrawlScope)(using globalScope: ConfigurationBuilder[T]): Unit =
      catchRecursiveCtx[CrawlContext]("crawl")
      crawlOp(block)

    /**
     * "crawl" level keyword for setting the exploration policy
     *
     * @param block   nested definitions of crawl settings
     * @param builder context provided by the "crawl" keyword
     */
    inline def policy(block: PolicyScope)(using builder: CrawlContext): Unit =
      catchRecursiveCtx[CrawlDocument]("policy")
      policyOp(block)

    extension (x: Iterable[URL])
      /**
       * Extension method to filter out URLs based on a predicate
       *
       * @param pred the predicate to filter URLs
       * @return a collection of URLs that do not match the predicate
       */
      infix def not(pred: URL => Boolean): Iterable[URL] =
        x.filterNot(pred)

  private[Crawl] object UnsafeOps:

    /**
     * Internal operation to handle the crawl configuration
     *
     * @param block   nested definitions of crawl settings
     * @param globalScope global scooby scope
     * @tparam T type of the configuration
     */
    def crawlOp[T](block: CrawlScope)(using globalScope: ConfigurationBuilder[T]): Unit =
      given builder: CrawlContext = CrawlContext("", globalScope.configuration.crawlerConfiguration.explorationPolicy)
      block
      globalScope.configuration = globalScope.configuration
        .focus(_.crawlerConfiguration.url)                 .replace(URL(builder.url))
        .focus(_.crawlerConfiguration.explorationPolicy)   .replace(builder.policy)

    /**
     * Internal operation to handle the exploration policy
     *
     * @param policy  the exploration policy to use
     * @param builder context provided by the "crawl" keyword
     */
    def policyOp(policy: PolicyScope)(using builder: CrawlContext): Unit =
    builder.policy = doc =>
      given CrawlDocument = doc
      policy

  /**
   * "crawl" level keyword for setting the URL to start crawling from
   *
   * @param init    the URL to start crawling from
   * @param builder context provided by the "crawl" keyword
   */
  def url(init: => String)(using builder: CrawlContext): Unit =
    builder.url = init

  /**
   * Retrieves all hyperlinks, present in the body, from the current crawl document context
   *
   * @param crawlDocumentContext context provided by the "policy" keyword
   * @return a collection of URLs representing hyperlinks
   */
  def hyperlinks(using crawlDocumentContext: CrawlDocument): Iterable[URL] = 
    crawlDocumentContext.frontier

  /**
   * Retrieves all links, in the entire html file (including head tag), from the current crawl document context
   *
   * @param crawlDocumentContext context provided by the "policy" keyword
   * @return a collection of URLs representing all link occurrences
   */
  def allLinks(using crawlDocumentContext: CrawlDocument): Iterable[URL] = 
    crawlDocumentContext.getAllLinkOccurrences

  /**
   * Predicate to check if a URL is external to the current document's domain
   *
   * @param document the current crawl document context
   * @return a function that checks if a URL is external
   */
  def external(using document: CrawlDocument): URL => Boolean =
    _.domain != document.url.domain
