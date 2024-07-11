package org.unibo.scooby
package core.scraper

import Aggregator.ItAggregator

/**
 * Class representing [[ScraperActor]]'s results.
 * @tparam T
 *   representing result's type.
 */
trait DataResult[T]:

  /**
   * Data structure used to store data.
   * @return
   *   data structure.
   */
  def data: Iterable[T]

  /**
   * Batch a single data to result.
   *
   * @param data
   *   single data to add.
   * @param aggregator
   *   data aggregator.
   * @return
   *   a new Result instance with data updated.
   */
  def updateStream(data: T)(using aggregator: ItAggregator[T]): DataResult[T]

  /**
   * Batch a sequence of data to result.
   *
   * @param data
   *   sequence of data to add.
   * @param aggregator
   *   data aggregator.
   * @return
   *   a new Result instance with data updated.
   */
  def updateBatch(data: Iterable[T])(using aggregator: ItAggregator[T]): DataResult[T]

  /**
   * Aggregate actual Result with a given one.
   *
   * @param result
   *   the Result to aggregate.
   * @param aggregator
   *   data aggregator.
   * @return
   *   a new Result instance with data aggregated.
   */
  def aggregate[D <: DataResult[T]](result: D)(using aggregator: ItAggregator[T]): DataResult[T]

/**
 * Class representing [[ScraperActor]]'s results implementation.
 * @param data
 *   representing actual result.
 * @tparam T
 *   representing result's type.
 */
final case class Result[T] (data: Iterable[T]) extends DataResult[T]:

  override def updateStream(data: T)(using aggregator: ItAggregator[T]): Result[T] =
    Result(aggregator.aggregateStream(this.data, data))

  override def updateBatch(data: Iterable[T])(using aggregator: ItAggregator[T]): Result[T] =
    Result(aggregator.aggregateBatch(this.data, data))

  override def aggregate[A <: DataResult[T]](result: A)(using aggregator: ItAggregator[T]): Result[T] =
    updateBatch(result.data)

/**
 * A companion object for [[Result]]
 */
object Result:
  
  /**
   * A builder with a starting data.
   *
   * @param data the starting singular data.
   * @tparam T the data type.
   * @return a new Result instance with given data.
   */
  def fromData[T](data: T): DataResult[T] = Result(Iterable(data))

  /**
   * A builder for an empty [[DataResult]].
   *
   * @tparam T the data type.
   * @return a new empty Result instance.
   */
  def empty[T]: DataResult[T] = Result(Iterable.empty[T])
