package org.unibo.scooby
package core.scooby

import core.coordinator.Coordinator
import core.crawler.{Crawler, CrawlerCommand}
import core.exporter.Exporter.*
import core.exporter.{Exporter, ExporterOptions}
import core.scraper.ScraperPolicies
import utility.http.URL

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

enum ScoobyCommand:
  case Start

object Scooby:
  import ScoobyCommand.*

  def run(): Unit =
    val scooby: ActorSystem[ScoobyCommand] = ActorSystem(ScoobyActor(), "Scooby")
    scooby ! Start


object ScoobyActor:
  import ScoobyCommand.*

  def apply(): Behavior[ScoobyCommand] =
    Behaviors.setup: context =>
      Behaviors.receiveMessage {
        case Start =>


          // 1. Spawn a coordinator
          val coordinator = context.spawn(Coordinator(), "Coordinator")

          // 2. Spawn an exporter
          val filePath = "test.txt"
          // TODO fix
//          val exporterOptions = ExporterOptions(_.data.toString, filePath)
//          val exporter = context.spawn(Exporter(exporterOptions), "Exporter")

          // 3. Spawn a crawler
          val crawler = context.spawn(Crawler(
            coordinator, 
            null, // TODO decomment exporter, 
            ScraperPolicies.scraperRule(Seq("body"), "tag"),
            _.frontier.map(URL(_).getOrElse(URL.empty))
          ), "Crawler")
          crawler ! CrawlerCommand.Crawl(URL("https://www.example.com").getOrElse(URL.empty))
          // 4. Send message to crawler containing the seed URL

          // (Behind the scenes) -> crawler will analyze urls (--> coordinator), create new crawlers (sub-urls) and new scrapers
          // (Behind the scenes) -> scraper send results to the exporter(s)

          // TODO: once finished we need to destroy the actor system.

          Behaviors.same

      }




object Main:
  def main(args: Array[String]): Unit =
    Scooby.run()

