package org.unibo.scooby
package core.coordinator

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

/**
 * The Coordinator object contains the definitions of the messages that the Coordinator actor can receive.
 */
object Coordinator:
  /**
   * The base trait for all messages that the Coordinator actor can receive.
   */
  sealed trait Command

  /**
   * A message that instructs the Coordinator actor to check a list of pages.
   *
   * @param pages   The list of pages to check.
   * @param replyTo The actor to send the result to.
   */
  final case class CheckPages(pages: List[String], replyTo: ActorRef[PagesChecked]) extends Command

  /**
   * A message that instructs the Coordinator actor to set a list of pages as crawled.
   *
   * @param pages The list of pages to set as crawled.
   */
  final case class SetCrawledPages(pages: List[String]) extends Command

  /**
   * A message that instructs the Coordinator actor to get the list of crawled pages.
   *
   * @param replyTo The actor to send the result to.
   */
  final case class GetCrawledPages(replyTo: ActorRef[List[String]]) extends Command

  /**
   * A message that contains the result of a CheckPages message.
   *
   * @param result A map of pages and their crawled status.
   */
  final case class PagesChecked(result: Map[String, Boolean])

  /**
   * The behavior of the Coordinator actor when it is first created.
   */
  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      new Coordinator(context).idle(Set.empty)
    }


/**
 * The Coordinator actor.
 *
 * @param context The context in which the actor is running.
 */
class Coordinator(context: ActorContext[Coordinator.Command]):

  import Coordinator._

  /**
   * The behavior of the Coordinator actor when it is idle.
   *
   * The method uses the Behaviors.receiveMessage function from the Akka Typed API to define the actor's
   * behavior when it receives a message. It matches on the type of the message and executes the
   * corresponding code block.
   *
   * @param crawledPages The set of pages that have been crawled. This is used to keep track of which
   *                     pages have already been crawled, so that the actor doesn't crawl the same page
   *                     multiple times.
   * @return A Behavior[Command] that describes how the actor should process the next message it receives.
   */
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

  /**
   * Normalizes a URL by removing the http:// or https:// prefix.
   *
   * @param url The URL to normalize.
   * @return The normalized URL.
   */
  private def normalizeURL(url: String): String = url.replaceFirst("^(http://|https://)", "")

  /**
   * Checks if a URL is valid.
   *
   * @param url The URL to check.
   * @return True if the URL is valid, false otherwise.
   */
  private def isValidURL(url: String): Boolean =
    val regex = "^(http://|https://)[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,}(\\S*)?$"
    url.matches(regex)

