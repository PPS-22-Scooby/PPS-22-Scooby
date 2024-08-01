package org.unibo.scooby
package core.scooby

import akka.actor.typed.*
import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import org.unibo.scooby.core.coordinator.{Coordinator, CoordinatorCommand}
import org.unibo.scooby.core.coordinator.CoordinatorCommand.SetupRobots
import org.unibo.scooby.core.crawler.{Crawler, CrawlerCommand}
import org.unibo.scooby.core.exporter.Exporter.*
import org.unibo.scooby.core.exporter.ExporterCommands.SignalEnd
import org.unibo.scooby.core.exporter.{Exporter, ExporterCommands, ExporterRouter}
import org.unibo.scooby.core.scooby.SingleExporting.{BatchExporting, StreamExporting}

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
   * Command to signal the correctly setup Robots.txt
   * @param found false if Robots.txt is empty or doesn't exists, true otherwise
   */
  case RobotsChecked(found: Boolean)

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

/**
 * Actor that represents the guardian actor for the Scooby application
 */
object ScoobyActor:
  import ScoobyCommand.*

  /**
   * Builds the main [[ScoobyActor]]'s behavior
   * @param configuration [[Configuration]] to be used for this application
   * @tparam T type of the [[Result]]s that will be exported
   * @return the behavior for the [[ScoobyActor]]
   */
  def apply[T](configuration: Configuration[T]): Behavior[ScoobyCommand] =
    Behaviors.setup: context =>
      Behaviors.receiveMessage:
        case Start =>

          // 1. Spawn a coordinator
          val coordinator = context.spawn(Coordinator(), "Coordinator")
          coordinator ! SetupRobots(configuration.crawlerConfiguration.url, context.self)
          waitRobotsChecked(configuration, coordinator)

        case RobotsChecked(_) =>
          Behaviors.stopped(() => println("Something wrong happened. Unexpected \"RobotsChecked\" received"))
        case ExportFinished =>
          Behaviors.stopped(() => println("Something wrong happened. Unexpected \"ExportFinished\" received"))

  /**
   * Waits for the Coordinator to have checked for the Robots.txt file at the root URL
   * @param configuration [[Configuration]] to be used for this application
   * @param coordinator coordinator Actor previously spawned
   * @tparam T type of the [[Result]]s that will be exported
   * @return the behavior for the [[ScoobyActor]] that waits for [[RobotsChecked]] and resumes the application starting
   *         behavior
   */
  private def waitRobotsChecked[T](configuration: Configuration[T],
                                   coordinator: ActorRef[CoordinatorCommand]): Behavior[ScoobyCommand] =
    Behaviors.setup: context =>
      Behaviors.receiveMessage:
        case RobotsChecked(found) =>
          // 2. Handle exporting
          val exporters = configuration.exporterConfiguration.exportingStrategies.zipWithIndex.map:
            case (SingleExporting.StreamExporting(behavior), index) =>
              context.spawn(Exporter.stream(behavior), s"Exporter$index-Stream")
            case (SingleExporting.BatchExporting(behavior, aggregation), index) =>
              context.spawn(Exporter.batch(behavior)(aggregation), s"Exporter$index-Batch")

          // 3. Spawn an ExporterRouter to broadcast exports messages
          val exporterRouter = context.spawn(ExporterRouter(exporters), "ExporterRouter")

          // 4. Spawn a crawler
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
        case Start =>
          Behaviors.stopped(() => println("Something wrong happened. Unexpected \"Start\" received"))
        case ExportFinished =>
          Behaviors.stopped(() => println("Something wrong happened. Unexpected \"ExportFinished\" received"))


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
