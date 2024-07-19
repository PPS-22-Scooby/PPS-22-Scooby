package org.unibo.scooby
package dsl
import core.exporter.Exporter.{AggregationBehaviors, ExportingBehaviors, Formats}
import core.scooby.Configuration.ExporterConfiguration
import core.scooby.SingleExporting.BatchExporting
import core.scooby.{Configuration, Scooby}
import core.scraper.Result
import utility.http.URL

import monocle.syntax.all.*

import scala.concurrent.{Future, Promise}
import scala.util.Success

trait ScoobyApplication extends App:
  export DSL.*
  def scooby[T](init: ConfigurationBuilder[T] ?=> Unit): Unit =
    given builder: ConfigurationBuilder[T] = new ConfigurationBuilder()
    // TODO currently having the write on console as default export, consider to leave or remove it
    builder.configuration = builder.configuration
      .focus(_.exporterConfiguration.exportingStrategies)     .replace(Seq(
        BatchExporting(ExportingBehaviors.writeOnConsole(Formats.string), AggregationBehaviors.default)
    ))
    init
    println(builder.configuration.crawlerConfiguration)
    Scooby.run(builder.build)


trait ScoobyEmbeddable:
  export DSL.*
  def scooby[T](init: ConfigurationBuilder[T] ?=> Unit): ScoobyRunnable[T] =
    given builder: ConfigurationBuilder[T] = new ConfigurationBuilder()
    init
    ScoobyRunnable(builder.build)


class ScoobyRunnable[T](config: Configuration[T]):
  def run(): Future[Result[T]] =
    println(config)
    val promise = Promise[Result[T]]()
    val promiseConfig = config
      .focus(_.exporterConfiguration.exportingStrategies)     .replace(Seq(
        BatchExporting((result: Result[T]) =>
          promise.complete(Success(result)), AggregationBehaviors.default)
    ))
    Scooby.run(promiseConfig)
    promise.future
