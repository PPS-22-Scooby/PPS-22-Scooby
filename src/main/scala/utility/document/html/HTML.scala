package org.unibo.scooby
package utility.document.html

import utility.document.Parser
import org.jsoup.Jsoup
import scala.jdk.CollectionConverters._

/**
 * Represents a HTML Document Object Model (DOM).
 *
 * @param htmlDocument
 *   the Jsoup Document instance
 */
class HTMLDom private (htmlDocument: org.jsoup.nodes.Document):
  /**
   * Selects HTML elements from the DOM using CSS selectors.
   *
   * @param selectors
   *   the CSS selectors
   * @return
   *   a sequence of HTMLElement instances
   */
  def select(selectors: String*): Seq[HTMLElement] =
    val selector = selectors.mkString(",")
    htmlDocument.select(selector).asScala.map(HTMLElement(_)).toSeq

  /**
   * Gets an HTML element from the DOM by its ID.
   *
   * @param id
   *   the ID of the element
   * @return
   *   an HTMLElement instance
   */
  def getElementById(id: String): HTMLElement =
    HTMLElement(htmlDocument.getElementById(id))

  /**
   * Gets HTML elements from the DOM by their tag name.
   *
   * @param tag
   *   the tag name
   * @return
   *   a sequence of HTMLElement instances
   */
  def getElementByTag(tag: String): Seq[HTMLElement] =
    htmlDocument.getElementsByTag(tag).asScala.map(HTMLElement(_)).toSeq

  /**
   * Gets HTML elements from the DOM by their class name.
   *
   * @param className
   *   the class name
   * @return
   *   a sequence of HTMLElement instances
   */
  def getElementByClass(className: String): Seq[HTMLElement] =
    htmlDocument.getElementsByClass(className).asScala.map(HTMLElement(_)).toSeq

/**
 * Represents an HTML element.
 *
 * @param htmlElement
 *   the Jsoup Element instance
 */
class HTMLElement private (htmlElement: org.jsoup.nodes.Element):
  /**
   * Gets the text of the HTML element.
   *
   * @return
   *   the text of the element
   */
  def text: String = htmlElement.text()

  /**
   * Gets the value of an attribute of the HTML element.
   *
   * @param attribute
   *   the name of the attribute
   * @return
   *   the value of the attribute
   */
  def attr(attribute: String): String = htmlElement.attr(attribute)

  /**
   * Gets the tag name of the HTML element.
   *
   * @return
   *   the tag name of the element
   */
  def tag: String = htmlElement.tagName()

  /**
   * Gets the outer HTML of the HTML element.
   *
   * @return
   *   the outer HTML of the element
   */
  def outerHtml: String = htmlElement.outerHtml()

object HTMLDom:
  private[html] def apply(htmlDocument: org.jsoup.nodes.Document): HTMLDom =
    new HTMLDom(htmlDocument)

object HTMLElement:
  private[html] def apply(htmlElement: org.jsoup.nodes.Element): HTMLElement =
    new HTMLElement(htmlElement)

/**
 * Provides a Parser instance for HTMLDom.
 */
given Parser[HTMLDom] = (content: String) => HTMLDom(Jsoup.parse(content))
