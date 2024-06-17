package org.unibo.scooby
package scraper

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import scala.jdk.CollectionConverters._
import scala.util.matching.Regex

object DummyScraper:

  object DataExtractor:

    def filterMultipleSelectors(document: Document, tags: Seq[String]): Seq[Element] =
      val selector = tags.mkString(",")
      document.select(selector).asScala.toSeq

    def filterSingleSelector(document: Document, tag: String): Seq[Element] =
      document.select(tag).asScala.toSeq

    def extract(document: Document): Map[String, String] =
      // Implementation depends on the structure of the HTML
      Map(
        "title" -> document.title(),
        "body" -> document.body().text()
      )

  object HtmlParser:
    def parse(html: String): Document = Jsoup.parse(html)

  class Scraper:
    // Suppose a regEx.yaml like
    // regex:
    //   - HTMLElem:
    //      textRegex: "^p.*x$".r
    //      attrRegex: attr => elem.has(attr) and attr == myAttr
    def parse(content: String): Document = HtmlParser.parse(content)
    def extractData(html: String): String = html

  trait FilterStrategy extends Scraper:
    override def extractData(html: String): String = html

  // Look for all HTML blocks in selectors
  trait SelectorsStrategy(selectors: Seq[String]) extends FilterStrategy:
    override def extractData(html: String): String =
      val document: Document = super.parse(html)

      val result: Seq[Element] = DataExtractor.filterMultipleSelectors(document, selectors)

      result.mkString("\n")

  // Look for all HTML blocks as regEx keys matching regEx value regex
  trait RegexStrategy(regEx: Map[String, Regex]) extends FilterStrategy:
    override def extractData(html: String): String =
      val document: Document = super.parse(html)

      val result: Seq[String] = List.empty

      regEx.foreachEntry:
        case (selector, reg) =>
          result :+ DataExtractor.filterSingleSelector(document, selector).filter(elem => reg.matches(elem.text()))

      result.mkString("\n")

  // Look for all HTML blocks in attributes keys which have attributes' value list of attributes
  // a that has ['class', 'id', ...] attributes
  trait AttributeStrategy(attributes: Map[String, Seq[String]]) extends FilterStrategy:
    override def extractData(html: String): String =
      val document: Document = super.parse(html)

      val result: Seq[String] = List.empty

      attributes.foreachEntry:
        case (selector, attr) =>
          result :+ DataExtractor.filterSingleSelector(document, selector).filter(elem =>attr.forall(elem.hasAttr))

      result.mkString("\n")
