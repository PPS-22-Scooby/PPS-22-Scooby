package org.unibo.scooby
package core.scraper

/**
 * Class representing {@link Scraper}'s results.
 * @param data representing actual result.
 * @tparam T representing result's type.
 */
class Result[T](private val data: Option[T]):

  /**
   * Returns true if data is defined, false otherwise.
   * @return true if data defined, false otherwise.
   */
  def isDefined: Boolean = data.isDefined

  /**
   * Returns result's data.
   * @return the data as {@link Option}
   */
  def getData: Option[T] = data

  /**
   * Batch a single data to result.
   * @param newData single data to add.
   * @param updater data aggregator.
   * @return a new Result instance with data updated.
   */
  def updateStream(newData: T)(implicit updater: (T, T) => T): Result[T] =
    data.fold(Result(newData))(elem => Result(updater(elem, newData)))

  /**
   * Batch a sequence of data to result.
   * @param newData sequence of data to add.
   * @param updater data aggregator.
   * @return a new Result instance with data updated.
   */
  def updateBatch(newData: Seq[T])(implicit updater: (T, T) => T): Result[T] =
    data.fold(Result(newData.reduce(updater)))(elem => Result(newData.foldLeft(elem)(updater)))

  /**
   * Aggregate actual Result with a given one.
   * @param newResult the Result to aggregate.
   * @param updater data aggregator.
   * @return a new Result instance with data aggregated.
   */
  def aggregate(newResult: Result[T])(implicit updater: (T, T) => T): Result[T] =
    newResult.data.fold(this)(elem => updateStream(elem)(updater))

/**
 * Companion object for Result class.
 */
object Result:

  /**
   * A builder with a starting data.
   * @param data the starting data.
   * @tparam T the data type.
   * @return a new Result instance with given data.
   */
  def apply[T](data: T): Result[T] = new Result(Some(data))


  /**
   * A builder with an empty data.
   * @tparam T the data type.
   * @return a new Result instance with empty data.
   */
  def apply[T](): Result[T] = new Result(None)

  /**
   * A string aggregator.
   */
  implicit val stringUpdater: (String, String) => String = (str1, str2) =>
    (str1, str2) match
      case ("", s2) => s2
      case (s1, "") => s1
      case (s1, s2) => s1 concat " " concat s2

  /**
   * An Int aggregator.
   */
  implicit val countUpdater: (Int, Int) => Int = _ + _

  /**
   * A Map aggregator.
   * @param map1 the first Map to aggregate.
   * @param map2 the second Map to aggregate.
   * @param aggregator the aggregator function used to aggregate values corresponding to the same keys of both given maps.
   * @tparam K the Maps' keys type.
   * @tparam V the Maps' values type.
   * @return the Map aggregated.
   */
  private def mergeMaps[K, V](map1: Map[K, V], map2: Map[K, V])(aggregator: (V, V) => V): Map[K, V] =
    (map1.keySet ++ map2.keySet).map {
      key =>
        val value = (map1.get(key), map2.get(key)) match {
          case (Some(val1), Some(val2))
            => aggregator(val1, val2)
          case (Some(val1), _)
            => val1
          case (_, Some(val2))
            => val2
          case _
            => throw new IllegalStateException("Something went wrong during maps merging.")
        }
        key -> value
    }.toMap

  /**
   * Return a map aggregator.
   * @param aggregator a Map's values aggregator function.
   * @tparam K Map's keys type.
   * @tparam V Map's values type.
   * @return the Map's aggregator.
   */
  implicit def mapUpdater[K, V](implicit aggregator: (V, V) => V): (Map[K, V], Map[K, V]) => Map[K, V] =
    (map1, map2) => mergeMaps(map1, map2)(aggregator)
