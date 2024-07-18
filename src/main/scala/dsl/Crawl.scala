package org.unibo.scooby
package dsl

import core.crawler.ExplorationPolicy
import dsl.DSL.ConfigurationBuilder
import utility.document.CrawlDocument
import utility.http.URL

import monocle.syntax.all.*

object Crawl:
  case class CrawlContext(var url: String, var policy: ExplorationPolicy)

  case class CrawlDocumentContext(var document: CrawlDocument)

  def crawl[T](init: CrawlContext ?=> Unit)(using context: ConfigurationBuilder[T]): Unit =
    given builder: CrawlContext = CrawlContext("", context.configuration.crawlerConfiguration.explorationPolicy)
    init
    context.configuration = context.configuration
      // TODO apply new URL
      .focus(_.crawlerConfiguration.url)      .replace(URL(builder.url).getOrElse(URL.empty))

  def url(init: => String)(using builder: CrawlContext): Unit =
    builder.url = init

  def policy(init: CrawlDocumentContext ?=> Iterable[URL])(using builder: CrawlContext): Unit =
    given documentContext: CrawlDocumentContext = CrawlDocumentContext(CrawlDocument("", URL.empty))
    // TODO find a way to implement links such as it brings the input document's links into the scope
    builder.policy = null

  def links(using builder: CrawlDocumentContext): Iterable[URL] =
    builder.document.frontier.map(URL(_).getOrElse(URL.empty))

