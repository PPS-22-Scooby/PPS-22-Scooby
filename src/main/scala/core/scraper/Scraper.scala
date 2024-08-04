package org.unibo.scooby
package core.scraper

import utility.document.ScrapeDocument
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import core.exporter.ExporterCommands
import ScraperPolicies.ScraperPolicy
import utility.http.URL

import scala.util.Try

/**
 * Enum representing all [[Scraper]]'s messages.
 */
enum ScraperCommands:
  case Scrape(doc: ScrapeDocument)

/**
 * Class representing Scraper actor.
 *
 * @param exporterRouter the exporter router [[ActorRef]] where to redirect the scraping results
 * @param scrapePolicy the scraping policy used by the actor.
 * @tparam T type representing the [[DataResult]] type.
 */
class Scraper[T](exporterRouter: ActorRef[ExporterCommands], scrapePolicy: ScraperPolicy[T]):

  import ScraperCommands._
  import core.exporter.ExporterCommands

  /**
   * Defines [[Scraper]]'s [[Behavior]].
   * @return the [[Behavior]]
   */
  def idle(): Behavior[ScraperCommands] =
    Behaviors.setup: context =>
      Behaviors.receiveMessage:
        case ScraperCommands.Scrape(doc: ScrapeDocument) =>
          Try:
            val res = resultFromPolicy(doc)
            exporterRouter ! ExporterCommands.Export(res)
          .fold(e => println(s"An error occurred while scraping: $e"), identity)
          Behaviors.stopped

  private def resultFromPolicy(argument: ScrapeDocument): Result[T] =
    Result(scrapePolicy(argument))

/**
 * Companion object for the Scraper actor.
 */
object Scraper:

  /**
   * Creates a new [[Scraper]] running actor.
   * @param exporterRouter the [[Exporter]] to send results to
   * @param scrapePolicy the [[ScraperPolicy]] of the [[Scraper]]
   * @tparam T result type of the [[ScraperPolicy]]
   * @return the [[Scraper]]'s [[Behavior]]
   */
  def apply[T](exporterRouter: ActorRef[ExporterCommands], scrapePolicy: ScraperPolicy[T]): Behavior[ScraperCommands] =
    Behaviors.setup {
      context => new Scraper(exporterRouter, scrapePolicy).idle()
    }

object ScraperPolicies:

  /**
   * A type representing a function that extract an [[Iterable]] used to build [[DataResult]] from a [[Document]]
   *
   * @tparam T a generic type which represents the expected [[DataResult]] type.
   */
  type ScraperPolicy[T] = ScrapeDocument => Iterable[T]

  extension [T1 <: String, T2](policy: ScraperPolicy[T1])

    /**
     * Concat two different policies.
     *
     * @param other the [[ScraperPolicy]] to concat.
     * @param docConverter the converter used to generate a [[Document]] which fits the other [[ScraperPolicy]].
     * @return the value obtained as concatenation of first and second [[ScraperPolicy]].
     */
    def concat(other: ScraperPolicy[T2])(using docConverter: (Iterable[T1], URL) => ScrapeDocument): ScraperPolicy[T2] = (doc: ScrapeDocument) =>
      val docConverted = docConverter(policy(doc), doc.url)
      other(docConverted)

  given ((Iterable[String], URL) => ScrapeDocument) = (doc: Iterable[String], url: URL) =>
    val content = doc.mkString("\n")
    ScrapeDocument(content, url)

  /**
   * Utility for [[ScraperPolicy]] based on selectBy attribute, given selectors specified.
   * Admissible values for selectBy are id, tag, class, css and regex.
   *
   * @param selectors a [[Seq]] of selectors used in scraper policy.
   * @param selectBy a selector to specify the policy.
   * @return the selected policy with specified selectors.
   */
  def scraperPolicy(selectors: Seq[String], selectBy: String): ScraperPolicy[String] = (scraper: ScrapeDocument) =>
    selectBy match
      case "id" =>
        selectors.map(scraper.getElementById).map(_.fold("")(_.outerHtml)).filter(_.nonEmpty)
      case "tag" =>
        selectors.flatMap(scraper.getElementsByTag).map(_.outerHtml)
      case "class" =>
        selectors.flatMap(scraper.getElementsByClass).map(_.outerHtml)
      case "css" =>
        selectors.flatMap(scraper.select(_)).map(_.outerHtml)
      case "regex" =>
        selectors.flatMap(scraper.find)
      case _ =>
        throw Error(s"Not yet implemented policy by $selectBy")

  /**
   * A [[ScraperPolicy]] based on elements' ids given.
   * @param ids a [[Seq]] of ids used in the policy.
   * @return the policy based on elements' ids.
   */
  def scraperIdSelectorPolicy(ids: Seq[String]): ScraperPolicy[String] = (scraper: ScrapeDocument) =>
    ids.map(scraper.getElementById).map(_.fold("")(_.outerHtml)).filter(_.nonEmpty)

  /**
   * A [[ScraperPolicy]] based on elements' tags given.
   *
   * @param tags a [[Seq]] of tags used in the policy.
   * @return the policy based on elements' tags.
   */
  def scraperTagSelectorPolicy(tags: Seq[String]): ScraperPolicy[String] = (scraper: ScrapeDocument) =>
    tags.flatMap(scraper.getElementsByTag).map(_.outerHtml)

  /**
   * A [[ScraperPolicy]] based on elements' classes given.
   *
   * @param classesNames a [[Seq]] of classes used in the policy.
   * @return the policy based on elements' classes.
   */
  def scraperClassSelectorPolicy(classesNames: Seq[String]): ScraperPolicy[String] = (scraper: ScrapeDocument) =>
    classesNames.flatMap(scraper.getElementsByClass).map(_.outerHtml)

  /**
   * A [[ScraperPolicy]] based on css selectors given.
   *
   * @param selectors a [[Seq]] of selectors used in the policy.
   * @return the policy based on css selectors.
   */
  def scraperCSSSelectorsPolicy(selectors: Seq[String]): ScraperPolicy[String] = (scraper: ScrapeDocument) =>
    selectors.flatMap(scraper.select(_)).map(_.outerHtml)

  /**
   * A [[ScraperPolicy]] based on regular expressions given.
   *
   * @param regex a [[Seq]] of regex used in the policy.
   * @return the policy based on regex.
   */
  def regexSelectorsPolicy(regex: Seq[String]): ScraperPolicy[String] = (scraper: ScrapeDocument) =>
    regex.flatMap(scraper.find)
