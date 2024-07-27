package org.unibo.scooby
package utility.document.html

import utility.document.Parser

import org.jsoup.Jsoup

import scala.jdk.CollectionConverters.*

/**
 * Represents a HTML Document Object Model (DOM).
 *
 * @param htmlDocument
 *   the Jsoup Document instance
 */
case class HTMLDom private (htmlDocument: org.jsoup.nodes.Document):
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
   *   an [[Option]] of [[HTMLElement]] instance
   */
  def getElementById(id: String): Option[HTMLElement] =
    Option(htmlDocument.getElementById(id)).collect:
      case element => HTMLElement(element)

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
    * Gets all the HTML elements from the DOM
    *
    * @return a [[Seq]] containing all the elements of the DOM
    */
  def allElements: Seq[HTMLElement] =
    // #root is a special node inserted by Jsoup, we ignore it
    htmlDocument.getAllElements.asScala.map(HTMLElement(_)).filterNot(_.tag == "#root").toSeq

/**
 * Represents an HTML element.
 *
 * @param htmlElement
 *   the Jsoup Element instance
 */
case class HTMLElement private (htmlElement: org.jsoup.nodes.Element):

  override def equals(obj: Any): Boolean = obj match {
    case that: HTMLElement => this.text.trim == that.text.trim
    case _ => false
  }

  override def hashCode(): Int = text.trim.hashCode
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
    * Gets the class names of this element
    *
    * @return a [[Set]] containing the class names
    */
  def classes: Set[String] = htmlElement.classNames().asScala.toSet

  /**
    * Gets the attributes of this elements
    *
    * @return a [[Map]] with the names of the attributes as keys and their values as values
    */
  def attributes: Map[String, String] = htmlElement.attributes().asScala
    .map(attr => (attr.getKey, attr.getValue)).toMap

  /**
   * Gets the tag name of the HTML element.
   *
   * @return
   *   the tag name of the element
   */
  def tag: String = htmlElement.tagName()

//  /**
//   * Extracts the attributes of the HTML element as a Map.
//   * This method converts the Jsoup Element's attributes into a Scala Map where each
//   * key-value pair represents an attribute name and its corresponding value.
//   *
//   * @return A Map[String, String] containing the attribute names and values.
//   */
//  def attributes: Map[String, String] =
//    htmlElement.attributes().asScala.map: attr =>
//      attr.getKey -> attr.getValue
//    .toMap

  /**
   * Retrieves the children of the HTML element as a sequence of HTMLElements.
   * This method iterates over the Jsoup Element's children, converting each to an HTMLElement,
   * allowing for Scala-friendly manipulation and access to the HTML structure.
   *
   * @return A Seq[HTMLElement] representing the children of the element.
   */
  def children: Seq[HTMLElement] = htmlElement.children().asScala.map(HTMLElement(_)).toSeq

  /**
   * Gets the outer HTML of the HTML element.
   *
   * @return
   *   the outer HTML of the element
   */
  def outerHtml: String = htmlElement.outerHtml()

  /**
    * Gets the id of this HTML element
    *
    * @return a [[String]] corresponding to the id
    */
  def id: String = htmlElement.id()

  /**
   * Gets the parent HTML element of this element.
   * @return the parent HTML element
   */
  def parent: HTMLElement = HTMLElement(htmlElement.parent())

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
