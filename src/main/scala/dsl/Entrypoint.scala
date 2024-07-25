package org.unibo.scooby
package dsl
import core.exporter.Exporter.{AggregationBehaviors, ExportingBehaviors, Formats}
import core.scooby.Configuration.ExporterConfiguration
import core.scooby.SingleExporting.BatchExporting
import core.scooby.{Configuration, Scooby}
import core.scraper.Result

import monocle.syntax.all.*

import scala.concurrent.{Future, Promise}
import scala.util.Success
import org.unibo.scooby.core.scooby.SingleExporting

trait ScoobyApplication extends App:
  export DSL.*
  def scooby[T](init: ConfigurationBuilder[T] ?=> Unit): Unit =
    given builder: ConfigurationBuilder[T] = new ConfigurationBuilder(Configuration.empty[T], ScrapingResultSetting[T]())
    // TODO currently having the write on console as default export, consider to leave or remove it
    builder.configuration = builder.configuration
      .focus(_.exporterConfiguration.exportingStrategies)     .replace(Seq(
        BatchExporting(ExportingBehaviors.writeOnConsole(Formats.string), AggregationBehaviors.default)
    ))
    init
    Scooby.run(builder.build)


trait ScoobyEmbeddable:
  export DSL.*
  def scooby[T](init: ConfigurationBuilder[T] ?=> Unit): ScoobyRunnable[T] =
    given builder: ConfigurationBuilder[T] = new ConfigurationBuilder(Configuration.empty[T], ScrapingResultSetting[T]())
    init
    ScoobyRunnable(builder.build)


class ScoobyRunnable[T](val config: Configuration[T]):
  def run(): Future[Result[T]] =
    val promise = Promise[Result[T]]()
    val promiseConfig = config
      .focus(_.exporterConfiguration.exportingStrategies)     .modify(_ ++ Seq(
        BatchExporting((result: Result[T]) =>
          promise.complete(Success(result)), AggregationBehaviors.default)
    ))
    Scooby.run(promiseConfig)
    promise.future
