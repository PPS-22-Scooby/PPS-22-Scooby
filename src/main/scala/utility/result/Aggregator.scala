package org.unibo.scooby
package utility.result

/**
 * Object used to provide aggregators to [[DataResult]].
 */
object Aggregator:

  /**
   * Aggregates single elements.
   * @tparam T
   *   type of elements to aggregate.
   */
  trait SingleElemAggregator[T]:

    /**
     * Aggregate elements provided.
     * @param x
     *   first element to aggregate.
     * @param y
     *   second element to aggregate.
     * @return
     *   aggregated element.
     */
    def aggregate(x: T, y: T): T

  /**
   * Aggregates elements provided as iterators.
   * @tparam T
   *   type of elements to aggregate.
   */
  trait ItAggregator[T]:

    /**
     * Aggregate elements provided both as [[Iterable]].
     * @param x
     *   first [[Iterable]] to aggregate.
     * @param y
     *   second [[Iterable]] to aggregate.
     * @return
     *   the [[Iterable]] obtained aggregating given ones.
     */
    def aggregateBatch(x: Iterable[T], y: Iterable[T]): Iterable[T]

    /**
     * Aggregate elements provided as [[Iterable]] and single element.
     *
     * @param x
     *   the [[Iterable]] to aggregate.
     * @param y
     *   the element to aggregate.
     * @return
     *   the [[Iterable]] obtained aggregating given ones.
     */
    def aggregateStream(x: Iterable[T], y: T): Iterable[T]

  /**
   * Provided [[Map]]'s aggregator.
   */
  given mapAggregator[K, V: SingleElemAggregator]: ItAggregator[(K, V)] with

    override def aggregateBatch(x: Iterable[(K, V)], y: Iterable[(K, V)]): Iterable[(K, V)] =
      aggregateBatch(x.toMap, y.toMap)

    override def aggregateStream(x: Iterable[(K, V)], y: (K, V)): Iterable[(K, V)] =
      aggregateStream(x.toMap, y)

    private def aggregateBatch(map1: Map[K, V], map2: Map[K, V]): Map[K, V] =
      (map1.keySet ++ map2.keySet).map {
        key =>
          val value = ((map1.get(key), map2.get(key)): @unchecked) match
            case (Some(val1), Some(val2)) =>
              summon[SingleElemAggregator[V]].aggregate(val1, val2)
            case (Some(val1), _) =>
              val1
            case (_, Some(val2)) =>
              val2
          key -> value
      }.toMap

    private def aggregateStream(map: Map[K, V], elem: (K, V)): Map[K, V] =
      map.get(elem._1) match
        case Some(value) =>
          map + (elem._1 -> summon[SingleElemAggregator[V]].aggregate(value, elem._2))
        case _ =>
          map + elem

  /**
   * Provided [[Seq]]'s aggregator.
   */
  given seqAggregator[T]: ItAggregator[T] with

    override def aggregateBatch(x: Iterable[T], y: Iterable[T]): Iterable[T] =
      aggregateBatch(x.toSeq, y.toSeq)

    override def aggregateStream(x: Iterable[T], y: T): Iterable[T] =
      aggregateStream(x.toSeq, y)

    private def aggregateBatch(seq1: Seq[T], seq2: Seq[T]): Seq[T] =
      seq1 ++ seq2

    private def aggregateStream(seq: Seq[T], elem: T): Seq[T] =
      seq :+ elem

  /**
   * A string aggregator.
   */
  given stringAggregator: SingleElemAggregator[String] with
    override def aggregate(str1: String, str2: String): String =
      (str1, str2) match
        case ("", s2) => s2
        case (s1, "") => s1
        case (s1, s2) => s1.concat(", ").concat(s2)

  /**
   * An Int aggregator.
   */
  given intAggregator: SingleElemAggregator[Int] with
    override def aggregate(x: Int, y: Int): Int = x + y
