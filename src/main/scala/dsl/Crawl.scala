package org.unibo.scooby
package dsl

import core.crawler.ExplorationPolicy
import dsl.DSL.ConfigurationBuilder
import utility.document.CrawlDocument
import utility.http.URL

import monocle.syntax.all.*
import _root_.dsl.syntax.catchRecursiveCtx

object Crawl:
  export SafeOps.*

  case class CrawlContext(var url: String, var policy: ExplorationPolicy)

  case class CrawlDocumentContext(var provider: ExplorationPolicy)

  object SafeOps:
    import UnsafeOps.* 

    inline def crawl[T](init: CrawlContext ?=> Unit)(using context: ConfigurationBuilder[T]): Unit =
      catchRecursiveCtx[CrawlContext]("crawl")
      crawlOp(init)

    inline def policy(init: CrawlDocument ?=> Iterable[URL])(using builder: CrawlContext): Unit =
      catchRecursiveCtx[CrawlDocument]("policy")
      policyOp(init)

  private[Crawl] object UnsafeOps:
    def crawlOp[T](init: CrawlContext ?=> Unit)(using context: ConfigurationBuilder[T]): Unit =
      given builder: CrawlContext = CrawlContext("", context.configuration.crawlerConfiguration.explorationPolicy)
      init
      context.configuration = context.configuration
        .focus(_.crawlerConfiguration.url)                 .replace(URL(builder.url))
        .focus(_.crawlerConfiguration.explorationPolicy)   .replace(builder.policy)

    def policyOp(init: CrawlDocument ?=> Iterable[URL])(using builder: CrawlContext): Unit =
    builder.policy = doc =>
      given CrawlDocument = doc
      init

  def url(init: => String)(using builder: CrawlContext): Unit =
    builder.url = init

  def hyperlinks(using crawlDocumentContext: CrawlDocument): Iterable[URL] = 
    crawlDocumentContext.frontier

  def allLinks(using crawlDocumentContext: CrawlDocument): Iterable[URL] = 
    crawlDocumentContext.getAllLinkOccurrences

  extension (x: Iterable[URL])
    infix def not(pred: URL => Boolean): Iterable[URL] = 
      x.filterNot(pred)

  def external(using document: CrawlDocument): URL => Boolean =
    _.domain != document.url.domain
