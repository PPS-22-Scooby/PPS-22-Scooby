package org.unibo.scooby
package core.scooby

import core.coordinator.Coordinator
import core.crawler.{Crawler, CrawlerCommand}
import core.exporter.Exporter.*
import core.exporter.{Exporter, ExporterCommands, ExportingBehavior}
import core.scooby.SingleExporting.{BatchExporting, StreamExporting}
import core.scraper.Scraper
import utility.document.Document
import utility.http.URL
import core.scooby.Configuration.{CrawlerConfiguration, ExporterConfiguration, ScraperConfiguration}

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import core.exporter.ExporterCommands.SignalEnd

enum ScoobyCommand:
  case Start

object Scooby:
  import ScoobyCommand.*

  def run[D <: Document, T](configuration: Configuration[D, T]): Unit =
    val scooby: ActorSystem[ScoobyCommand] = ActorSystem(ScoobyActor(configuration), "Scooby")
    scooby ! Start


object ScoobyActor:
  import ScoobyCommand.*

  def apply[D <: Document, T](configuration: Configuration[D, T]): Behavior[ScoobyCommand] =
    Behaviors.setup: context =>
      Behaviors.receiveMessage:
        case Start =>


          // 1. Spawn a coordinator
          val coordinator = context.spawn(Coordinator(), "Coordinator")

          // 2. Spawn an exporter
          val exporters = configuration.exporterConfiguration.exportingStrategies.zipWithIndex.map {
            case (SingleExporting.StreamExporting(behavior), index) =>
              context.spawn(Exporter.stream(behavior), s"Exporter${index}-Stream")
            case (SingleExporting.BatchExporting(behavior, aggregation), index) =>
              context.spawn(Exporter.batch(behavior)(aggregation), s"Exporter${index}-Batch")
          }

          // 3. Spawn a crawler
          val crawler = context.spawn(Crawler(
            coordinator,
            exporters.head, // TODO fix crawler and scraper to take multiple exporters
            configuration.scraperConfiguration.scrapePolicy,
            configuration.crawlerConfiguration.explorationPolicy,
            configuration.crawlerConfiguration.maxDepth
          ), s"RootCrawler-${Crawler.getCrawlerName(configuration.crawlerConfiguration.url)}")
          crawler ! CrawlerCommand.Crawl(configuration.crawlerConfiguration.url)

          context.watch(crawler)
          // 4. Send message to crawler containing the seed URL

          // (Behind the scenes) -> crawler will analyze urls (--> coordinator), create new crawlers (sub-urls) and new scrapers
          // (Behind the scenes) -> scraper send results to the exporter(s)

          // TODO: once finished we need to destroy the actor system.

          waitCrawlingFinished(exporters)

  private def waitCrawlingFinished(exporters: Iterable[ActorRef[ExporterCommands]]): Behavior[ScoobyCommand] =
    Behaviors.receiveSignal:
      case (context, Terminated(child)) =>
        // TODO exploit ask pattern to wait for all exporter to have finished exporting
        exporters.foreach(_ ! SignalEnd())

        onFinishedExecution()
        Behaviors.stopped


  def onFinishedExecution(): Unit =
    // kill actor system
    println("Process end with success!")


object Main:
  def main(args: Array[String]): Unit =
    Scooby.run(
      Configuration(
        CrawlerConfiguration(
          URL("https://www.example.com").getOrElse(URL.empty),
          _.frontier.map(URL(_).getOrElse(URL.empty)),
          2,
          utility.http.Configuration.default
        ),
        ScraperConfiguration(Scraper.scraperRule(Seq("link"), "tag")),
        ExporterConfiguration(Seq(
          BatchExporting(
            ExportingBehaviors.writeOnConsole(Formats.string),
            AggregationBehaviors.default
          )))
      )
    )

