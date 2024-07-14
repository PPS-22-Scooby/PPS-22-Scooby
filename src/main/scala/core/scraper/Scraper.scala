package org.unibo.scooby
package core.scraper

import utility.document.ScrapeDocument

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import core.exporter.ExporterCommands

import ScraperPolicies.ScraperPolicy
import utility.http.URL

/**
 * Enum representing all [[Scraper]]'s messages.
 */
enum ScraperCommands:
  case Scrape(doc: ScrapeDocument)

/**
 * Class representing Scraper actor.
 *
 * @param scrapeRule the scraping rule the actor uses.
 * @tparam T type representing the [[DataResult]] type.
 */
class Scraper[T](val exporter: ActorRef[ExporterCommands], val scrapeRule: ScraperPolicy[T]):

  import ScraperCommands._
  import core.exporter.ExporterCommands

  def idle(): Behavior[ScraperCommands] =
    Behaviors.setup: context =>
      Behaviors.receiveMessage:
        case ScraperCommands.Scrape(doc: ScrapeDocument) =>
          val res = resultFromRule(doc)
          exporter ! ExporterCommands.Export(res)
          Behaviors.stopped



  private def resultFromRule(argument: ScrapeDocument): Result[T] =
    Result(scrapeRule(argument))

/**
 * Companion object for the Scraper actor.
 */
object Scraper:

  def apply[T](exporter: ActorRef[ExporterCommands], scrapeRule: ScraperPolicy[T]): Behavior[ScraperCommands] =
    Behaviors.setup {
      context => new Scraper(exporter, scrapeRule).idle()
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
   * Utility for scraper's rules based on selectBy attribute, given selectors specified.
   * Admissible values are id, tag, class and css.
   *
   * @param selectors a [[Seq]] of selectors used in scraper rule.
   * @param selectBy a selector to specify the rule.
   * @return the selected rule with specified selectors.
   */
  def scraperRule(selectors: Seq[String], selectBy: String): ScraperPolicy[String] = (scraper: ScrapeDocument) =>
    selectBy match
      case "id" =>
        selectors.map(scraper.getElementById).map(_.fold("")(_.outerHtml)).filter(_.nonEmpty)
      case "tag" =>
        selectors.flatMap(scraper.getElementByTag).map(_.outerHtml)
      case "class" =>
        selectors.flatMap(scraper.getElementByClass).map(_.outerHtml)
      case "css" =>
        selectors.flatMap(scraper.select(_)).map(_.outerHtml)
      case "regex" =>
        selectors.flatMap(scraper.find)
      case _ =>
        throw Error(s"Not yet implemented rule by $selectBy")

  /**
   * A scraper rule based on elements' ids given.
   * @param ids a [[Seq]] of ids used in the rule.
   * @return the rule based on elements' ids.
   */
  def scraperIdSelectorRule(ids: Seq[String]): ScraperPolicy[String] = (scraper: ScrapeDocument) =>
    ids.map(scraper.getElementById).map(_.fold("")(_.outerHtml)).filter(_.nonEmpty)

  /**
   * A scraper rule based on elements' tags given.
   *
   * @param tags a [[Seq]] of tags used in the rule.
   * @return the rule based on elements' tags.
   */
  def scraperTagSelectorRule(tags: Seq[String]): ScraperPolicy[String] = (scraper: ScrapeDocument) =>
    tags.flatMap(scraper.getElementByTag).map(_.outerHtml)

  /**
   * A scraper rule based on elements' classes given.
   *
   * @param classesNames a [[Seq]] of classes used in the rule.
   * @return the rule based on elements' classes.
   */
  def scraperClassSelectorRule(classesNames: Seq[String]): ScraperPolicy[String] = (scraper: ScrapeDocument) =>
    classesNames.flatMap(scraper.getElementByClass).map(_.outerHtml)

  /**
   * A scraper rule based on css selectors given.
   *
   * @param selectors a [[Seq]] of selectors used in the rule.
   * @return the rule based on css selectors.
   */
  def scraperCSSSelectorsRule(selectors: Seq[String]): ScraperPolicy[String] = (scraper: ScrapeDocument) =>
    selectors.flatMap(scraper.select(_)).map(_.outerHtml)

  /**
   * A scraper rule based on regular expressions given.
   *
   * @param regex a [[Seq]] of regex used in the rule.
   * @return the rule based on regex.
   */
  def regexSelectorsRule(regex: Seq[String]): ScraperPolicy[String] = (scraper: ScrapeDocument) =>
    regex.flatMap(scraper.find)
