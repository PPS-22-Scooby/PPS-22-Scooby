package org.unibo.scooby
package crawler

import scala.util.matching.Regex

trait Parser[A]:
  def parse(content: String): A

trait DocumentType[P <: Parser[_]]:
  def frontier()(using parser: P): Seq[String]



final case class CrawlDocument(url: String, content: String)
    extends DocumentType[Parser[Seq[String]]]:

  override def frontier()(using parser: Parser[Seq[String]]): Seq[String] =
    parser.parse(content)


object CrawlDocument:

  given linkParser: Parser[Seq[String]] = (content: String) =>
    """<a\b[^>]*href="([^#][^"]*)"""".r.findAllMatchIn(content).map(_.group(1)).toSeq
