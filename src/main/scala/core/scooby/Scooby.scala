package org.unibo.scooby
package core.scooby

import core.coordinator.Coordinator
import core.crawler.{Crawler, CrawlerCommand, ExplorationPolicies}
import core.exporter.Exporter.*
import core.exporter.ExporterCommands.SignalEnd
import core.exporter.{Exporter, ExporterCommands, ExporterRouter}
import core.scooby.Configuration.{CoordinatorConfiguration, CrawlerConfiguration, ExporterConfiguration, ScraperConfiguration}
import core.scooby.SingleExporting.{BatchExporting, StreamExporting}
import core.scraper.ScraperPolicies
import utility.http.{ClientConfiguration, URL}

import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.*
import akka.util.Timeout

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future}

/**
 * Main commands to be used inside Scooby
 */
enum ScoobyCommand:
  /**
   * Starts the application
   */
  case Start

  /**
   * Command to signal the end of this exporter behavior
   */
  case ExportFinished

object Scooby:
  import ScoobyCommand.*

  /**
   * Runs the Scooby application.
   * @param configuration configuration to be used for this application
   * @tparam T type of the [[Result]]s that will be exported
   */
  def run[T](configuration: Configuration[T]): Unit =
    val scooby: ActorSystem[ScoobyCommand] = ActorSystem(ScoobyActor(configuration), "Scooby")
    scooby ! Start
    Await.result(scooby.whenTerminated, Duration.Inf)


object ScoobyActor:
  import ScoobyCommand.*

  /**
   * Builds the main [[ScoobyActor]]'s behavior
   * @param configuration configuration to be used for this application
   * @tparam T type of the [[Result]]s that will be exported
   * @return the behavior for the [[ScoobyActor]]
   */
  def apply[T](configuration: Configuration[T]): Behavior[ScoobyCommand] =
    Behaviors.setup: context =>
      Behaviors.receiveMessage:
        case Start =>

          // 1. Spawn a coordinator
          val coordinator = context.spawn(Coordinator(), "Coordinator")

          // 2. Handle exporting

          val exporters = configuration.exporterConfiguration.exportingStrategies.zipWithIndex.map {
            case (SingleExporting.StreamExporting(behavior), index) =>
              context.spawn(Exporter.stream(behavior), s"Exporter${index}-Stream")
            case (SingleExporting.BatchExporting(behavior, aggregation), index) =>
              context.spawn(Exporter.batch(behavior)(aggregation), s"Exporter${index}-Batch")
          }

          val exporterRouter = context.spawn(ExporterRouter(exporters), "ExporterRouter")


          // 3. Spawn a crawler
          val crawler = context.spawnAnonymous(Crawler(
            coordinator,
            exporterRouter,
            configuration.scraperConfiguration.scrapePolicy,
            configuration.crawlerConfiguration.explorationPolicy,
            configuration.crawlerConfiguration.maxDepth,
            configuration.crawlerConfiguration.networkOptions
          ))
          crawler ! CrawlerCommand.Crawl(configuration.crawlerConfiguration.url)

          context.watch(crawler)
          waitCrawlingFinished(exporters)

  /**
   * Waiting behavior for the [[ScoobyActor]]. Waits until all the crawling is finished
   * @param exporters exporters that need to notified when the crawling is finished
   * @return the behavior that waits until all the crawling is finished
   */
  private def waitCrawlingFinished(exporters: Seq[ActorRef[ExporterCommands]]): Behavior[ScoobyCommand] =
    Behaviors.receiveSignal:
      case (context, Terminated(child)) =>
        implicit val timeout: Timeout = Timeout(5.seconds)
        implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
        implicit val scheduler: Scheduler = context.system.scheduler
        val toWait = Future.sequence(exporters.map(_ ? SignalEnd.apply))
        Await.result(toWait, Duration.Inf)
        onFinishedExecution()
        context.system.terminate()
        Behaviors.stopped

  /**
   * Callback for when the application ends its execution.
   */
  private def onFinishedExecution(): Unit =
    println("Process end with success!")


object Main:
  def main(args: Array[String]): Unit =
    Scooby.run(
      Configuration(
        CrawlerConfiguration(
          URL("https://www.example.com"),
          ExplorationPolicies.allLinks,
          2,
          ClientConfiguration.default
        ),
        ScraperConfiguration(ScraperPolicies.scraperRule(Seq("link"), "tag")),
        ExporterConfiguration(Seq(
          BatchExporting(
            ExportingBehaviors.writeOnConsole(Formats.string),
            AggregationBehaviors.default
          ))),
        CoordinatorConfiguration(100)
      )
    )

