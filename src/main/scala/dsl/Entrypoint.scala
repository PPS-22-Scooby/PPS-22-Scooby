package org.unibo.scooby
package dsl
import core.scooby.{Configuration, Scooby}

import core.scraper.Result
import core.scooby.SingleExporting.BatchExporting
import scala.util.Success
import core.exporter.Exporter.AggregationBehaviors

import scala.concurrent.{Future, Promise}

trait ScoobyApplication extends App:
  export DSL.*
  def scooby[T](init: ConfigurationBuilder[T] ?=> Unit): Unit =
    given builder: ConfigurationBuilder[T] = new ConfigurationBuilder()
    Scooby.run(builder.build)


trait ScoobyEmbeddable:
  export DSL.*
  def scooby[T](init: => Configuration[T]): ScoobyRunnable[T] =
    ScoobyRunnable(init)
    
    
class ScoobyRunnable[T](config: Configuration[T]):
  def run(): Future[Result[T]] =
    val promise = Promise[Result[T]]()
    val promiseConfig = config.copy(exporterConfiguration =
      config.exporterConfiguration.copy(exportingStrategies = Seq(
        BatchExporting((result: Result[T]) =>
          promise.complete(Success(result)), AggregationBehaviors.default))
      )
    )
    Scooby.run(promiseConfig)
    promise.future
