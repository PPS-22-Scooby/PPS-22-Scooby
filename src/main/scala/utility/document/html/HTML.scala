package org.unibo.scooby
package utility.document.html

import utility.document.Parser
import org.jsoup.Jsoup
import scala.jdk.CollectionConverters._


class HTMLDom private(htmlDocument: org.jsoup.nodes.Document):
  def select(selectors: String*): Seq[HTMLElement] =
    val selector = selectors.mkString(",")
    htmlDocument.select(selector).asScala.map(HTMLElement(_)).toSeq
  def getElementById(id: String): HTMLElement =
    HTMLElement(htmlDocument.getElementById(id))
  def getElementByTag(tag: String): Seq[HTMLElement] =
    htmlDocument.getElementsByTag(tag).asScala.map(HTMLElement(_)).toSeq
  def getElementByClass(className: String): Seq[HTMLElement] =
    htmlDocument.getElementsByClass(className).asScala.map(HTMLElement(_)).toSeq

class HTMLElement private(htmlElement: org.jsoup.nodes.Element):
  def text: String = htmlElement.text()
  def attr(attribute: String): String = htmlElement.attr(attribute)
  def tag: String = htmlElement.tagName()
  def outerHtml: String = htmlElement.outerHtml()

object HTMLDom:
  private[html] def apply(htmlDocument: org.jsoup.nodes.Document): HTMLDom =
    new HTMLDom(htmlDocument)
    
object HTMLElement:
  private[html] def apply(htmlElement: org.jsoup.nodes.Element): HTMLElement =
    new HTMLElement(htmlElement)
    
  
given Parser[HTMLDom] = (content: String) => HTMLDom(Jsoup.parse(content))

