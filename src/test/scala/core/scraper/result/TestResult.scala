package org.unibo.scooby
package core.scraper.result

import core.scraper.{DataResult, Result}

import org.scalatest.matchers.should.Matchers
import core.scraper.DataResult
import core.scraper.Aggregator.given
import core.scraper.Aggregator.SingleElemAggregator

import org.scalatest.wordspec.AnyWordSpecLike

class TestResult extends AnyWordSpecLike with Matchers:

  "Result" should:
    "initialize with given data" in:
      val expectedList = List(1, 2, 3)
      val expectedMap = Map("first" -> 1, "second" -> 2, "third" -> 3)
      val expectedSeq = Seq(1, 2, 3)
      val expectedIt = Iterable(1, 2, 3)

      val resultList = Result(expectedList)
      val resultMap = Result(expectedMap)
      val resultSeq = Result(expectedSeq)
      val resultIt = Result(expectedIt)

      resultList.data should be(expectedList)
      resultMap.data should be(expectedMap)
      resultSeq.data should be(expectedSeq)
      resultIt.data should be(expectedIt)

    "support adding a single item to the data" in :
      val startList = List(1, 2, 3)
      val startMap = Map("first" -> 1, "second" -> 2, "third" -> 3)
      val startSeq = Seq(1, 2, 3)
      val startIt = Iterable(1, 2, 3)

      val resultList = Result(startList)
      val resultMap = Result(startMap)
      val resultSeq = Result(startSeq)
      val resultIt = Result(startIt)

      val updateList = 4
      val updateMap = ("forth", 4)
      val updateSeq = 4
      val updateIt = 4

      val expectedList = startList :+ updateList
      val expectedMap = startMap + updateMap
      val expectedSeq = startSeq :+ updateSeq
      val expectedIt = startIt ++ Iterable(updateIt)

      resultList.updateStream(updateList).data should be(expectedList)
      resultMap.updateStream(updateMap).data should be(expectedMap)
      resultSeq.updateStream(updateSeq).data should be(expectedSeq)
      resultIt.updateStream(updateIt).data should be(expectedIt)

    "support adding a sequence of items to the data" in:
      val startList = List(1, 2, 3)
      val startMap = Map("first" -> 1, "second" -> 2, "third" -> 3)
      val startSeq = Seq(1, 2, 3)
      val startIt = Iterable(1, 2, 3)

      val resultList = Result(startList)
      val resultMap = Result(startMap)
      val resultSeq = Result(startSeq)
      val resultIt = Result(startIt)

      val updateList = List(4, 5)
      val updateMap = Map("forth" -> 4, "fifth" -> 5)
      val updateSeq = Seq(4, 5)
      val updateIt = Iterable(4, 5)

      val expectedList = startList ++ updateList
      val expectedMap = startMap ++ updateMap
      val expectedSeq = startSeq ++ updateSeq
      val expectedIt = startIt ++ updateIt

      resultList.updateBatch(updateList).data should be(expectedList)
      resultMap.updateBatch(updateMap).data should be(expectedMap)
      resultSeq.updateBatch(updateSeq).data should be(expectedSeq)
      resultIt.updateBatch(updateIt).data should be(expectedIt)

    "combine data from two results" in :
      val startList1 = List(1, 2, 3)
      val startMap1 = Map("first" -> 1, "second" -> 2, "third" -> 3)
      val startSeq1 = Seq(1, 2, 3)
      val startIt1 = Iterable(1, 2, 3)

      val resultList1 = Result(startList1)
      val resultMap1 = Result(startMap1)
      val resultSeq1 = Result(startSeq1)
      val resultIt1 = Result(startIt1)

      val startList2 = List(4, 5)
      val startMap2 = Map("forth" -> 4, "fifth" -> 5)
      val startSeq2 = Seq(4, 5)
      val startIt2 = Iterable(4, 5)

      val resultList2 = Result(startList2)
      val resultMap2 = Result(startMap2)
      val resultSeq2 = Result(startSeq2)
      val resultIt2 = Result(startIt2)

      val expectedList = startList1 ++ startList2
      val expectedMap = startMap1 ++ startMap2
      val expectedSeq = startSeq1 ++ startSeq2
      val expectedIt = startIt1 ++ startIt2

      resultList1.aggregate(resultList2).data should be(expectedList)
      resultMap1.aggregate(resultMap2).data should be(expectedMap)
      resultSeq1.aggregate(resultSeq2).data should be(expectedSeq)
      resultIt1.aggregate(resultIt2).data should be(expectedIt)

    "support map aggregation even with shared keys" in:
      val startMap1 = Map("first" -> 1, "second" -> 2, "third" -> 3)
      val startMap2 = Map("third" -> 4, "fifth" -> 5)

      val resultMap1 = Result(startMap1)
      val resultMap2 = Result(startMap2)

      val notExpectedMap = startMap1 ++ startMap2
      val expectedMap = (startMap1.keySet ++ startMap2.keySet).map {
        key =>
          val value = (startMap1.get(key), startMap2.get(key)) match
            case (Some(val1), Some(val2)) =>
              summon[SingleElemAggregator[Int]].aggregate(val1, val2)
            case (Some(val1), _) =>
              val1
            case (_, Some(val2)) =>
              val2
          key -> value
      }.toMap

      resultMap1.aggregate(resultMap2).data should not be notExpectedMap
      resultMap1.updateBatch(startMap2).data should not be notExpectedMap

      resultMap1.aggregate(resultMap2).data should be(expectedMap)
      resultMap1.updateBatch(startMap2).data should be(expectedMap)
