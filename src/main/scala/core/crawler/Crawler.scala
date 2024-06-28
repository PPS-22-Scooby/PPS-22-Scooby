package org.unibo.scooby
package core.crawler

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import utility.http.{Deserializer, Request, Response, URL}

import akka.actor.typed.{ActorRef, Behavior}
import utility.http.Clients.SimpleHttpClient
import utility.document.CrawlDocument

import core.coordinator.CoordinatorCommand


enum CrawlerCommand:
  case Crawl(url: URL)
  case CrawlerCoordinatorResponse(result: Map[String, Boolean])

object Crawler:
  def apply(coordinator: ActorRef[CoordinatorCommand] ): Behavior[CrawlerCommand] = Behaviors.setup :
    context => new Crawler(context, coordinator).idle()

class Crawler(context: ActorContext[CrawlerCommand], coordinator: ActorRef[CoordinatorCommand]):
  import CrawlerCommand._

  import Deserializer.default
  given httpClient: SimpleHttpClient = SimpleHttpClient()

  def idle(): Behavior[CrawlerCommand] = Behaviors.receiveMessage:
    case Crawl(url) =>
      Request.builder.get().at(url).send match
        case Left(s: String) => context.log.error(s"Error while crawling $url: $s")
        case Right(response: Response) =>
          val links: Seq[String] = new CrawlDocument(response.body.get, url).frontier
          this.coordinator ! CoordinatorCommand.CheckPages(links.toList, context.self)
      Behaviors.same
    case CrawlerCoordinatorResponse(links) =>
      context.log.info(s"Received links: $links")
      Behaviors.same

