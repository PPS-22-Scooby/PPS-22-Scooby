package org.unibo.scooby
package dsl

import utility.document.html.HTMLElement

import utility.document.{CommonHTMLExplorer, Document, RegExpExplorer, SelectorExplorer}

object HTML:
  import _root_.dsl.syntax.catchRecursiveCtx

  private type RuleDefinitionScope = HTMLElement ?=> Boolean
  private type HTMLElementRule = HTMLElement => Boolean

  /**
   * Utility keyword to specify a rule that the obtained HTML
   * elements must attained to. Usable inside blocks like "scrape" or "crawl"
   *
   * @param block definition of the rule
   * @return a rule that filters HTML elements
   */
  inline def rule(block: RuleDefinitionScope): HTMLElementRule =
    catchRecursiveCtx[HTMLElement]("rule")
    el =>
      given HTMLElement = el
      block

  def element(using el: HTMLElement): HTMLElement = el

  /**
   * Retrieves tag from an [[HTMLElement]]
   *
   * @return the tag as [[String]].
   */
  def tag: HTMLElement => String = _.tag

  /**
   * Retrieves text from an [[HTMLElement]]
   *
   * @return text as [[String]].
   */
  def text: HTMLElement => String = _.text

  /**
   * Retrieves outer Html text from an [[HTMLElement]]
   *
   * @return outer Html text as [[String]].
   */
  def outerText: HTMLElement => String = _.outerHtml

  /**
   * Retrieves value of the specified attribute of an [[HTMLElement]].
   *
   * @param attribute the attribute to read the value.
   * @return the value of the attribute as [[String]].
   */
  def attr(attribute: String): HTMLElement => String = _.attr(attribute)

  /**
   * Utility keyword to retrieve the HTML elements contained in the HTML document.
   *
   * @param documentContext document present in this context
   * @tparam D type of document present in this scope (e.g. CrawlDocument, ScrapeDocument ecc.)
   * @return an [[Iterable]] of all the HTML elements contained in the document
   */
  def elements[D <: Document & CommonHTMLExplorer](using documentContext: D): Iterable[HTMLElement] =
    documentContext.getAllElements


  def document[T <: Document & CommonHTMLExplorer](using document: T): T = document

  def matchesOf[T <: Document & RegExpExplorer](regExp: String)(using documentContext: T): Iterable[String] =
    documentContext.find(regExp)

  def select[T <: Document & SelectorExplorer](selectors: String*)(using documentContext: T): Iterable[HTMLElement] =
    documentContext.select(selectors *)

  def classes: HTMLElement => Iterable[String] = _.classes

  def attributes: HTMLElement => Iterable[(String, String)] = _.attributes

  def id: HTMLElement => String = _.id

  infix def dont[T](predicate: T => Boolean): T => Boolean = (elem) => !predicate(elem)

  infix def haveTag(tag: String): HTMLElement => Boolean = _.tag == tag

  infix def haveClass(cssClass: String): HTMLElement => Boolean = _.classes.contains(cssClass)

  infix def haveId(id: String): HTMLElement => Boolean = _.id == id

  infix def haveAttribute(attributeName: String): HTMLElement => Boolean = _.attr(attributeName).nonEmpty

  infix def haveAttributeValue(attributeName: String, attributeValue: String): HTMLElement => Boolean =
    _.attr(attributeName) == attributeValue
