package org.unibo.scooby
package dsl

import core.scooby.SingleExporting.{BatchExporting, StreamExporting}
import core.scraper.Result
import dsl.DSL.ConfigurationBuilder

import monocle.syntax.all.*
import org.unibo.scooby.core.exporter.Exporter.AggregationBehaviors

object Export:

  case class ExportationContext[T](builder: ConfigurationBuilder[T]):
    given ConfigurationBuilder[T] = builder
    infix def as(init: Iterable[T] ?=> Unit)(using builder: ConfigurationBuilder[T]): Unit =
      builder.configuration = builder.configuration
        .focus(_.exporterConfiguration.exportingStrategies)      .replace(
          Seq(BatchExporting((res: Result[T]) =>
            given Iterable[T] = res.data
            init
        , AggregationBehaviors.default)))


  infix def exports[T](using builder: ConfigurationBuilder[T]): ExportationContext[T] =
    ExportationContext[T](builder)

  case class ExportContext[T]()

  def results[T](using context: Iterable[T]): Iterable[T] = context.asInstanceOf[Iterable[T]]





