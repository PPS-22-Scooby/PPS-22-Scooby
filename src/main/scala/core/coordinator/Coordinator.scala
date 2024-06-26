package org.unibo.scooby
package core.coordinator

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

object Coordinator:

  sealed trait Command
  final case class CheckPages(pages: List[String], replyTo: ActorRef[PagesChecked]) extends Command
  final case class SetCrawledPages(pages: List[String]) extends Command
  final case class GetCrawledPages(replyTo: ActorRef[List[String]]) extends Command
  final case class PagesChecked(result: Map[String, Boolean])

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      new Coordinator(context).idle(Set.empty)
    }

class Coordinator(context: ActorContext[Coordinator.Command]):

  import Coordinator._

  private def idle(crawledPages: Set[String]): Behavior[Command] =
    Behaviors.receiveMessage {
      case CheckPages(pages, replyTo) =>
        val (updatedPages, result) = pages.foldLeft((crawledPages, Map.empty[String, Boolean])) {
          case ((updatedPages, result), page) =>
            val normalizedPage = normalizeURL(page)
            val isCrawled = updatedPages.contains(normalizedPage)
            if (isValidURL(page) && !isCrawled) {
              (updatedPages + normalizedPage, result + (page -> false))
            } else {
              (updatedPages, result + (page -> isCrawled))
            }
        }
        replyTo ! PagesChecked(result)
        idle(updatedPages)

      case SetCrawledPages(pages) =>
        val validPages = pages.filter(isValidURL).map(normalizeURL).toSet
        idle(crawledPages ++ validPages)

      case GetCrawledPages(replyTo) =>
        replyTo ! crawledPages.toList
        Behaviors.same
    }

  private def normalizeURL(url: String): String = url.replaceFirst("^(http://|https://)", "")

  private def isValidURL(url: String): Boolean =
    val regex = "^(http://|https://)[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,}(\\S*)?$"
    url.matches(regex)

