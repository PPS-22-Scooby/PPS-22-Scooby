package org.unibo.scooby
package core.scraper.result

import core.scraper.ResultImpl
import core.scraper.Aggregator.given

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import core.scraper.Aggregator.SingleElemAggregator

class TestResult extends AnyFunSuite with Matchers:

  test("ResultImpl should initialize with given data"):
    val expectedList = List(1, 2, 3)
    val expectedMap = Map("first" -> 1, "second" -> 2, "third" -> 3)
    val expectedSeq = Seq(1, 2, 3)
    val expectedIt = Iterable(1, 2, 3)

    val resultList = ResultImpl(expectedList)
    val resultMap = ResultImpl(expectedMap)
    val resultSeq = ResultImpl(expectedSeq)
    val resultIt = ResultImpl(expectedIt)

    resultList.data should be (expectedList)
    resultMap.data should be (expectedMap)
    resultSeq.data should be (expectedSeq)
    resultIt.data should be (expectedIt)

  test("updateStream should add a single item to the data"):
    val startList = List(1, 2, 3)
    val startMap = Map("first" -> 1, "second" -> 2, "third" -> 3)
    val startSeq = Seq(1, 2, 3)
    val startIt = Iterable(1, 2, 3)

    val resultList = ResultImpl(startList)
    val resultMap = ResultImpl(startMap)
    val resultSeq = ResultImpl(startSeq)
    val resultIt = ResultImpl(startIt)

    val updateList = 4
    val updateMap = ("forth", 4)
    val updateSeq = 4
    val updateIt = 4

    val expectedList = startList :+ updateList
    val expectedMap = startMap + updateMap
    val expectedSeq = startSeq :+ updateSeq
    val expectedIt = startIt ++ Iterable(updateIt)

    resultList.updateStream(updateList).data should be (expectedList)
    resultMap.updateStream(updateMap).data should be (expectedMap)
    resultSeq.updateStream(updateSeq).data should be (expectedSeq)
    resultIt.updateStream(updateIt).data should be (expectedIt)

  test("updateBatch should add a sequence of items to the data"):
    val startList = List(1, 2, 3)
    val startMap = Map("first" -> 1, "second" -> 2, "third" -> 3)
    val startSeq = Seq(1, 2, 3)
    val startIt = Iterable(1, 2, 3)

    val resultList = ResultImpl(startList)
    val resultMap = ResultImpl(startMap)
    val resultSeq = ResultImpl(startSeq)
    val resultIt = ResultImpl(startIt)

    val updateList = List(4, 5)
    val updateMap = Map("forth" -> 4, "fifth" -> 5)
    val updateSeq = Seq(4, 5)
    val updateIt = Iterable(4, 5)

    val expectedList = startList ++ updateList
    val expectedMap = startMap ++ updateMap
    val expectedSeq = startSeq ++ updateSeq
    val expectedIt = startIt ++ updateIt

    resultList.updateBatch(updateList).data should be (expectedList)
    resultMap.updateBatch(updateMap).data should be (expectedMap)
    resultSeq.updateBatch(updateSeq).data should be (expectedSeq)
    resultIt.updateBatch(updateIt).data should be (expectedIt)

  test("aggregate should combine data from two results"):
    val startList1 = List(1, 2, 3)
    val startMap1 = Map("first" -> 1, "second" -> 2, "third" -> 3)
    val startSeq1 = Seq(1, 2, 3)
    val startIt1 = Iterable(1, 2, 3)

    val resultList1 = ResultImpl(startList1)
    val resultMap1 = ResultImpl(startMap1)
    val resultSeq1 = ResultImpl(startSeq1)
    val resultIt1 = ResultImpl(startIt1)

    val startList2 = List(4, 5)
    val startMap2 = Map("forth" -> 4, "fifth" -> 5)
    val startSeq2 = Seq(4, 5)
    val startIt2 = Iterable(4, 5)

    val resultList2 = ResultImpl(startList2)
    val resultMap2 = ResultImpl(startMap2)
    val resultSeq2 = ResultImpl(startSeq2)
    val resultIt2 = ResultImpl(startIt2)

    val expectedList = startList1 ++ startList2
    val expectedMap = startMap1 ++ startMap2
    val expectedSeq = startSeq1 ++ startSeq2
    val expectedIt = startIt1 ++ startIt2

    resultList1.aggregate(resultList2).data should be (expectedList)
    resultMap1.aggregate(resultMap2).data should be (expectedMap)
    resultSeq1.aggregate(resultSeq2).data should be (expectedSeq)
    resultIt1.aggregate(resultIt2).data should be (expectedIt)

  test("map aggregation for shared keys"):
    val startMap1 = Map("first" -> 1, "second" -> 2, "third" -> 3)
    val startMap2 = Map("third" -> 4, "fifth" -> 5)

    val resultMap1 = ResultImpl(startMap1)
    val resultMap2 = ResultImpl(startMap2)

    val notExpectedMap = startMap1 ++ startMap2
    val expectedMap = (startMap1.keySet ++ startMap2.keySet).map {
      key =>
        val value = (startMap1.get(key), startMap2.get(key)) match {
          case (Some(val1), Some(val2)) =>
            summon[SingleElemAggregator[Int]].aggregate(val1, val2)
          case (Some(val1), _) =>
            val1
          case (_, Some(val2)) =>
            val2
        }
        key -> value
    }.toMap

    resultMap1.aggregate(resultMap2).data should not be notExpectedMap
    resultMap1.updateBatch(startMap2).data should not be notExpectedMap

    resultMap1.aggregate(resultMap2).data should be (expectedMap)
    resultMap1.updateBatch(startMap2).data should be (expectedMap)
