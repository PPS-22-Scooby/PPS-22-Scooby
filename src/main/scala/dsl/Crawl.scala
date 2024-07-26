package org.unibo.scooby
package dsl

import core.crawler.ExplorationPolicy
import dsl.DSL.ConfigurationBuilder
import utility.document.CrawlDocument
import utility.http.URL

import monocle.syntax.all.*
import dsl.syntax.catchRecursiveCtx

object Crawl:
  export SafeOps.*

  case class CrawlContext(var url: String, var policy: ExplorationPolicy)

  private type CrawlScope = CrawlContext ?=> Unit
  private type PolicyScope = CrawlDocument ?=> Iterable[URL]

  object SafeOps:
    import UnsafeOps.* 

    inline def crawl[T](block: CrawlScope)(using context: ConfigurationBuilder[T]): Unit =
      catchRecursiveCtx[CrawlContext]("crawl")
      crawlOp(block)

    inline def policy(blocK: PolicyScope)(using builder: CrawlContext): Unit =
      catchRecursiveCtx[CrawlDocument]("policy")
      policyOp(blocK)

    extension (x: Iterable[URL])
      infix def not(pred: URL => Boolean): Iterable[URL] =
        x.filterNot(pred)

  private[Crawl] object UnsafeOps:
    def crawlOp[T](block: CrawlScope)(using context: ConfigurationBuilder[T]): Unit =
      given builder: CrawlContext = CrawlContext("", context.configuration.crawlerConfiguration.explorationPolicy)
      block
      context.configuration = context.configuration
        .focus(_.crawlerConfiguration.url)                 .replace(URL(builder.url))
        .focus(_.crawlerConfiguration.explorationPolicy)   .replace(builder.policy)

    def policyOp(block: PolicyScope)(using builder: CrawlContext): Unit =
    builder.policy = doc =>
      given CrawlDocument = doc
      block

  def url(init: => String)(using builder: CrawlContext): Unit =
    builder.url = init

  def hyperlinks(using crawlDocumentContext: CrawlDocument): Iterable[URL] = 
    crawlDocumentContext.frontier

  def allLinks(using crawlDocumentContext: CrawlDocument): Iterable[URL] = 
    crawlDocumentContext.getAllLinkOccurrences

  def external(using document: CrawlDocument): URL => Boolean =
    _.domain != document.url.domain
