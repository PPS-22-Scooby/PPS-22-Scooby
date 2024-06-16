package org.unibo.scooby
package crawler

import org.unibo.scooby.crawler.TransformationContext.Response
import sttp.client3.{SimpleHttpClient, UriContext, basicRequest}
import CrawlDocument.given

trait DataAcquisition:
  def exploreUrl(url: String)(using transformer: DocumentTransformer[Response]): CrawlDocument

trait Crawling:
  def crawl(): Unit


trait DocumentTransformer[A]:
  def transform(value: A): CrawlDocument

object TransformationContext:
  type Response = sttp.client3.Response[Either[String, String]]

  given crawlDocumentTransformer: DocumentTransformer[Response] with
      def transform(value: Response): CrawlDocument =
        val body = value.body.getOrElse("")
        CrawlDocument(value.request.uri.toString, body)

final case class Crawler(seedUrl: String) extends DataAcquisition with Crawling:
  import TransformationContext.{given, *}
  import CrawlDocument.given

  override def exploreUrl(url: String)(using transformer: DocumentTransformer[Response]): CrawlDocument =
    transformer.transform(fetchUrl(url))

  override def crawl(): Unit =
    val seedDocument = exploreUrl(seedUrl)
    // check coordinator
    // ...
    val outLinks = seedDocument.frontier()

    // spawn crawlers
    // ...


def fetchUrl(url: String) : Response =
  val client = SimpleHttpClient()
  client.send(basicRequest.get(uri"${url}"))


object Test extends App:
  Crawler("https://it.wikipedia.org/wiki/Rickrolling").crawl()


