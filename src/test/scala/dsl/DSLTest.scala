package org.unibo.scooby
package dsl

import Application.scooby
import core.crawler.ExplorationPolicies
import core.exporter.Exporter.{AggregationBehaviors, ExportingBehaviors, Formats}
import core.scooby.Configuration.{CoordinatorConfiguration, CrawlerConfiguration, ExporterConfiguration, ScraperConfiguration}
import core.scooby.SingleExporting.BatchExporting
import core.scooby.{Configuration, Scooby}
import core.scraper.ScraperPolicies
import core.scooby.Main
import dsl.util.ScoobyTest
import utility.document.html.HTMLElement
import utility.document.{CommonHTMLExplorer, CrawlDocument, Document, ScrapeDocument}
import utility.http.{ClientConfiguration, URL}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.{be, should}

import scala.concurrent.Await
import scala.concurrent.duration.{DurationInt, FiniteDuration}

//class DSLTest extends ScoobyTest:

//  val tout: FiniteDuration = 9.seconds
//  val maxRequest: Int = 5
//  val auth: (String, String) = ("Authorization", "prova")
//  val agent: (String, String) = ("Agent", "gr")
//  val maxDepth: Int = 2
//  val maxLinks: Int = 20
//  val url: String = "https://www.example.com"
//  val fileDSL: String = "test-DSL.txt"
//  val fileStandard: String = "test-standard.txt"
//
//  def linksPolicy(using CrawlDocument): Iterable[URL] => Iterable[URL] = link => link not (external)
//
//  def scrapePredicate: HTMLElement => Boolean = haveAttribute("href") and dont:
//    haveAttributeValue("href", "/domains/reserved") or
//      haveAttributeValue("href", "/about")
//        .and:
//          followRule {
//            element.tag == "a"
//          } and followRule {
//            element.tag == "div"
//          }
//
//  def scrapePolicy[D <: Document & CommonHTMLExplorer, T]: D ?=> Iterable[HTMLElement] = elements.filter(scrapePredicate)
//
//  def batchStrategy: Iterable[HTMLElement] ?=> Unit = results get tag output :
//    toConsole withFormat Text
//    toFile(fileDSL) withFormat Text
//
//  def batchAggregation[T]: (Iterable[T], Iterable[T]) => Iterable[T] = _ ++ _
//
//  def streamingStrategy: Iterable[HTMLElement] ?=> Unit = results.groupBy(_.tag).view.mapValues(_.size).toMap output :
//    toConsole withFormat Text
//
//  "Application with DSL configurations" should "run correctly" in :
//
//    val app = scooby:
//      config:
//        network:
//          Timeout is tout
//          MaxRequests is maxRequest
//          headers:
//            auth._1 to auth._2
//            agent._1 to agent._2
//        option:
//          MaxDepth is maxDepth
//          MaxLinks is maxLinks
//
//      crawl:
//        url:
//          this.url
//        policy:
//          linksPolicy(hyperlinks)
//      scrape:
//        scrapePolicy
//      exports:
//        batch:
//          strategy:
//            batchStrategy
//
//          aggregate:
//            batchAggregation
//
//    //      streaming:
//    //        streamingStrategy
//
//    val result = Await.result(app.run(), 10.seconds)
//    println(result)
//
//  "Application with standard configurations" should "run correctly" in :
//
//    Scooby.run(
//      Configuration(
//        CrawlerConfiguration(
//          URL("https://www.example.com"), //url),
//          ExplorationPolicies.allLinks,
//          2,
//          ClientConfiguration.default
//        ),
//        ScraperConfiguration(ScraperPolicies.scraperRule(Seq("link"), "tag")),
//        ExporterConfiguration(Seq(
//          BatchExporting(
//            ExportingBehaviors.writeOnConsole(Formats.string), //File(Path.of(fileStandard), Formats.string),
//            AggregationBehaviors.default
//          ))),
//        CoordinatorConfiguration(100)
//      )
//    )
