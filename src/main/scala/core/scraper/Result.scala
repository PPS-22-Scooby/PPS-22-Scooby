package org.unibo.scooby
package core.scraper

import Aggregator.ItAggregator

/**
 * Class representing {@link Scraper}'s results.
 * @param data representing actual result.
 * @tparam T representing result's type.
 */
class Result[T](private val data: Iterable[T]):

  /**
   * Returns result's data.
   * @return the data.
   */
  def getData: Iterable[T] = data

  /**
   * Batch a single data to result.
   * @param newData single data to add.
   * @param aggregator data aggregator.
   * @return a new Result instance with data updated.
   */
  def updateStream(newData: T)(using aggregator: ItAggregator[T]): Result[T] =
    Result(aggregator.aggregateStream(data, newData))

  /**
   * Batch a sequence of data to result.
   * @param newData sequence of data to add.
   * @param aggregator data aggregator.
   * @return a new Result instance with data updated.
   */
  def updateBatch(newData: Iterable[T])(using aggregator: ItAggregator[T]): Result[T] =
    Result(aggregator.aggregateBatch(data, newData))

  /**
   * Aggregate actual Result with a given one.
   * @param newResult the Result to aggregate.
   * @param aggregator data aggregator.
   * @return a new Result instance with data aggregated.
   */
  def aggregate(newResult: Result[T])(using aggregator: ItAggregator[T]): Result[T] =
    updateBatch(newResult.data)

/**
 * Companion object for Result class.
 */
object Result:

  /**
   * A builder with a starting data.
   *
   * @param data the starting data iterable.
   * @tparam T the data type.
   * @return a new Result instance with given data.
   */
  def apply[T](data: Iterable[T]): Result[T] = new Result(data)
