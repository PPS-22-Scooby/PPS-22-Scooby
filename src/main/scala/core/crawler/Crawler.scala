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
        case Left(s: String) =>
          context.log.error(s"Error while crawling $url: $s")

        case Right(response: Response) =>
          println(response.headers.get("content-type"))
          response.headers.get("content-type") match
            case Some(contentType) if contentType.startsWith("text/") =>
              val links: Seq[String] = new CrawlDocument(response.body.get, url).frontier
              this.coordinator ! CoordinatorCommand.CheckPages(links.toList, context.self)
            case _ =>
              context.log.error(s"$url does not have a text content type")


      Behaviors.same
    case CrawlerCoordinatorResponse(links) =>
      context.log.info(s"Received links: $links")
      links.foreach :
          case (returnedUrl, false) => URL(returnedUrl) match
            case Right(url) =>
              val childName = s"crawler-${url.domain}"
              val children = context.spawn(Crawler(coordinator), childName)
              children ! Crawl(url)
            case _ => ()
          case _ => ()
      Behaviors.same

