package org.unibo.scooby
package core.crawler

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import utility.http.{Deserializer, Request, Response, URL}

import akka.actor.typed.{ActorRef, Behavior}
import utility.http.Clients.SimpleHttpClient
import utility.document.CrawlDocument

import core.coordinator.CoordinatorCommand


enum CrawlerCommand:
  /**
   * Command to initiate crawling of the given URL.
   *
   * @param url the URL to be crawled
   */
  case Crawl(url: URL)

  /**
   * Command to handle the response from the coordinator with the result of checked pages.
   *
   * @param result a map of URLs to their corresponding statuses
   */
  case CrawlerCoordinatorResponse(result: Iterator[String])

object Crawler:
  /**
   * Creates a new Crawler actor.
   *
   * @param coordinator the ActorRef of the coordinator to communicate with
   * @return the behavior of the Crawler actor
   */
  def apply(coordinator: ActorRef[CoordinatorCommand] ): Behavior[CrawlerCommand] = Behaviors.setup :
    context => new Crawler(context, coordinator).idle()

/**
 * Class representing a Crawler actor.
 *
 * @param context     the ActorContext of the Crawler actor
 * @param coordinator the ActorRef of the coordinator to communicate with
 */
class Crawler(context: ActorContext[CrawlerCommand], coordinator: ActorRef[CoordinatorCommand]):
  import CrawlerCommand._

  val httpClient: SimpleHttpClient = SimpleHttpClient()

  /**
   * The behavior of the Crawler actor.
   *
   * @return the behavior of the Crawler actor
   */
  def idle(): Behavior[CrawlerCommand] = Behaviors.receiveMessage:
    case Crawl(url) =>
      Request.builder.get().at(url).build match
        case Left(s: String) =>
          context.log.error(s"Error while crawling $url: $s")

        case Right(request: Request) =>
          request.send(httpClient) match
            case Left(s: String) =>
              context.log.error(s"Error while crawling $url: $s")
            case Right(response: Response) =>
              response.headers.get("content-type") match
                case Some(contentType) if contentType.startsWith("text/") =>
                  val links: Seq[String] = new CrawlDocument(response.body.get, url).frontier
                  this.coordinator ! CoordinatorCommand.CheckPages(links.toList, context.self)
                case _ =>
                  context.log.error(s"$url does not have a text content type")
      Behaviors.same

    case CrawlerCoordinatorResponse(links) =>
      context.log.info(s"Received links: $links")
      for 
        returnedUrl <- links
        url <- URL(returnedUrl).toOption
      do
        val childName = s"crawler-${url.domain}"
        val children = context.spawn(Crawler(coordinator), childName)
        children ! Crawl(url)
      
      Behaviors.same

