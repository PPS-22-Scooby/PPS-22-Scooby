package org.unibo.scooby
package core.crawler

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import utility.http.URL

import akka.actor.typed.Behavior


enum CrawlerCommand:
  case Crawl(url: URL)
  case CrawlerCoordinatorResponse(links: Set[URL])

class Crawler(context: ActorContext[CrawlerCommand]):
  import CrawlerCommand._
  
  def apply(): Behavior[CrawlerCommand] = Behaviors.receiveMessage:
    case Crawl(url) =>
      context.log.info(s"Crawling $url")
      Behaviors.same
    case CrawlerCoordinatorResponse(links) =>
      context.log.info(s"Received links: $links")
      Behaviors.same