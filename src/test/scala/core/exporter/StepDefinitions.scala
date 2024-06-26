package org.unibo.scooby
package core.exporter

import core.exporter.Exporter
import core.exporter.Exporter.*
import org.junit.Assert.assertEquals

import io.cucumber.datatable.DataTable
import io.cucumber.scala.{EN, ScalaDsl}

class StepDefinitions extends ScalaDsl with EN:

  private var exporter: Exporter[Any] = _

  private var results: List[DataTable] = List()

  private var stringResult = ""

  Given("""I have an Exporter with a {string} strategy""") { (format: String) =>
    exporter = format match
      case "listCount" => ListCountExporter()// new Exporter() with ListCountStrategy()
      case "csvCount" => CsvExporter()// new Exporter() with CsvCountStrategy()
      case "json" => JsonExporter()
      // case _ => new Exporter()
  }

  And("""the following result""") { (input: DataTable) =>
    exporter = exporter <-- input
  }

  And("""the following result of many""") { (input: DataTable) =>
    results = input :: results
  }

  When("""I try to export it""") { () =>
    stringResult = exporter.exportData()
  }
  
  Then("""it should return {string}""") { (result: String) =>
    assertEquals(result, stringResult)
  }

  Then("""it should return first {string} and then {string}""") { (result1: String, result2: String) =>
    exporter = exporter <-- results.tail.head
    assertEquals(result1, exporter.exportData())
    exporter = exporter <-- results.head
    assertEquals(result2, exporter.exportData())
  }

