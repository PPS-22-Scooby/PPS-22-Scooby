package org.unibo.scooby
package dsl
import core.scooby.{Configuration, Scooby}
import core.scraper.{Result, ScraperPolicies}
import core.scooby.SingleExporting.BatchExporting

import scala.util.Success
import core.exporter.Exporter.{AggregationBehaviors, ExportingBehaviors, Formats}

import org.unibo.scooby.core.scooby.Configuration.{CrawlerConfiguration, ExporterConfiguration, ScraperConfiguration}
import org.unibo.scooby.utility.http.{ClientConfiguration, URL}

import scala.concurrent.{Future, Promise}

// TODO change into default Configuration provider
def PLACEHOLDER[T]: Configuration[T] = Configuration(
  CrawlerConfiguration(
    URL("https://www.example.com").getOrElse(URL.empty),
    _.frontier.map(URL(_).getOrElse(URL.empty)),
    2,
    ClientConfiguration.default
  ),
  null,
  ExporterConfiguration(Seq(
    BatchExporting(
      ExportingBehaviors.writeOnConsole(Formats.string),
      AggregationBehaviors.default
    )))
)

trait ScoobyApplication extends App:
  export DSL.*
  def scooby[T](init: ConfigurationBuilder[T] ?=> Unit): Unit =
    given builder: ConfigurationBuilder[T] = new ConfigurationBuilder(PLACEHOLDER)
    init
    Scooby.run(builder.build)


trait ScoobyEmbeddable:
  export DSL.*
  def scooby[T](init: ConfigurationBuilder[T] ?=> Unit): ScoobyRunnable[T] =
    given builder: ConfigurationBuilder[T] = new ConfigurationBuilder(PLACEHOLDER)
    init
    ScoobyRunnable(builder.build)


class ScoobyRunnable[T](config: Configuration[T]):
  def run(): Future[Result[T]] =
    println(config.crawlerConfiguration.networkOptions)
    val promise = Promise[Result[T]]()
    val promiseConfig = config.copy(exporterConfiguration =
      config.exporterConfiguration.copy(exportingStrategies = Seq(
        BatchExporting((result: Result[T]) =>
          promise.complete(Success(result)), AggregationBehaviors.default))
      )
    )
    Scooby.run(promiseConfig)
    promise.future
