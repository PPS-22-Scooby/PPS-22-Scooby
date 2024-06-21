package org.unibo.scooby
package core.scraper

object Result:

  def apply[T](data: T): Result[T] = new Result(Some(data))
  def apply[T](): Result[T] = new Result(None)

  implicit val stringUpdater: (String, String) => String = _ concat "\n" concat _
  implicit val countUpdater: (Int, Int) => Int = _ + _

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

  // Implicit updater for Map[K, V] with a custom aggregator
  implicit def mapUpdater[K, V](implicit aggregator: (V, V) => V): (Map[K, V], Map[K, V]) => Map[K, V] =
    (map1, map2) => mergeMaps(map1, map2)(aggregator)

final case class Result[T](private val data: Option[T]):
  def isDefined: Boolean = data.isDefined

  def getData: Option[T] = data

  def updateStream(newData: T)(implicit updater: (T, T) => T): Result[T] =
    data.fold(Result(newData))(elem => Result(updater(elem, newData)))

  def updateBatch(newData: Seq[T])(implicit updater: (T, T) => T): Result[T] =
    data.fold(Result(newData.reduce(updater)))(elem => Result(newData.foldLeft(elem)(updater)))

  def aggregate(newResult: Result[T])(implicit updater: (T, T) => T): Result[T] =
    newResult.data.fold(Result(data))(elem => updateStream(elem)(updater))
