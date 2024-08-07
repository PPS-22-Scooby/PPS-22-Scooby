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
import core.scooby.SingleExporting
import org.unibo.scooby.dsl.DSL.ConfigurationWrapper

/**
 * Generated an empty [[ConfigurationWrapper]]
 *
 * @tparam T type of the wrapped [[Configuration]]
 * @return an empty [[ConfigurationWrapper]]
 */
private inline def emptyWrapper[T]: ConfigurationWrapper[T] =
  new ConfigurationWrapper(Configuration.empty[T])

/**
 * Generates a new [[Configuration]] that also includes a default standard output exporting. Useful, for example, to
 * let the user skip the "export" part of the DSL
 * @param wrapper [[ConfigurationWrapper]] whose [[Configuration]] is used as starting point
 * @tparam T type of the wrapped [[Configuration]]
 * @return a new [[Configuration]] including also a default export
 */
private def includeDefaultConsoleExporting[T](wrapper: ConfigurationWrapper[T]): Configuration[T] =
  wrapper.configuration
    .focus(_.exporterConfiguration.exportingStrategies).replace(Seq(
      BatchExporting(ExportingBehaviors.writeOnConsole(Formats.string), AggregationBehaviors.default)
    ))

/**
 * Alias type representing the top-level scope of the Scooby DSL (i.e. section under scooby: ...)
 * @tparam T type of the [[ConfigurationWrapper]] (and therefore also the one of [[Configuration]])
 */
private type ScoobyScope[T] = ConfigurationWrapper[T] ?=> Unit

/**
 * Entry point trait for using Scooby as an application. It contains all of Scooby DSL keywords. It extends [[scala.App]]
 * and can therefore be used directly as executable object.
 * == Example usage ==
 * {{{
 *   import org.unibo.scooby.dsl.ScoobyApplication
 *
 *   object Example extends ScoobyApplication:
 *
 *      scooby:
 *        ...
 * }}}
 *
 */
trait ScoobyApplication extends App:
  export DSL.{*, given}

  /**
   * Entry point of the DSL inside a [[ScoobyApplication]]. Scooby will be run immediately using the configuration
   * defined inside it
   * @param block the definition of the [[Configuration]] through the DSL
   * @tparam T the type of results defined by the scraping behavior
   */
  def scooby[T](block: ScoobyScope[T]): Unit =
    given wrapper: ConfigurationWrapper[T] = emptyWrapper
    wrapper.configuration = includeDefaultConsoleExporting(wrapper)
    block
    Scooby.run(wrapper.value)

/**
 * Entry point trait for using Scooby as a library. It contains all of Scooby DSL keywords. This is given as an
 * alternative to [[ScoobyApplication]] for when Scooby is not meant to be used as a standalone library but instead is
 * part of a project that needs to use its features for something else.
 *
 * This trait, differently from [[ScoobyApplication]], can instantiate a Scooby application and obtain its results as
 * a [[Future]]
 *
 * == Example usage ==
 * {{{
 *   import org.unibo.scooby.dsl.ScoobyEmbeddable
 *   import org.unibo.scooby.core.scraper.Result
 *   import scala.concurrent.Await
 *   import scala.concurrent.duration.Duration
 *
 *   class Example extends ScoobyEmbeddable:
 *
 *      def exampleMethod: Unit =
 *
 *        val app =
 *          scooby:
 *            ...
 *
 *        val finalResult: Result[?] = Await.result(app.run(), Duration.Inf)
 *        println(finalResult.data)
 * }}}
 *
 * == Side note ==
 * Even if scooby lets you write an "exports" part inside [[ScoobyEmbeddable]] (and that section will be, in fact,
 * correctly executed) the results that `app.run` returns will be the ones returned by the scraping, therefore ignoring
 * the defined exporting strategies.
 */
trait ScoobyEmbeddable:
  export DSL.{*, given}

  /**
   * Entry point of the DSL inside a [[ScoobyEmbeddable]]. Scooby will encapsulate the defined configuration inside a
   * [[ScoobyRunnable]] that can be run later.
   * @param block the definition of the [[Configuration]] through the DSL
   * @tparam T the type of results defined by the scraping behavior
   * @return a [[ScoobyRunnable]] encapsulating the defined configuration
   */
  def scooby[T](block: ScoobyScope[T]): ScoobyRunnable[T] =
    given wrapper: ConfigurationWrapper[T] = emptyWrapper
    block
    ScoobyRunnable(wrapper.value)

/**
 * Runnable Scooby [[Configuration]]. Used by [[ScoobyEmbeddable]].
 * @param config [[Configuration]] to be run
 * @tparam T type of the [[Configuration]]
 */
class ScoobyRunnable[T](val config: Configuration[T]):
  /**
   * Runs this Runnable returning a [[Future]] containing the [[Result]]s obtained by the Scooby application.
   * Final results ignore the exporting strategies defined in the configuration (see [[ScoobyEmbeddable]] for more
   * details)
   * @return a [[Future]] containing the results
   */
  def run(): Future[Result[T]] =
    val promise = Promise[Result[T]]()
    val promiseConfig = config
      .focus(_.exporterConfiguration.exportingStrategies)     .modify(_ ++ Seq(
        BatchExporting((result: Result[T]) =>
          promise.complete(Success(result)), AggregationBehaviors.default)
    ))
    Scooby.run(promiseConfig)
    promise.future
