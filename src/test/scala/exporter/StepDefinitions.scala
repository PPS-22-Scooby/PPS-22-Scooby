package org.unibo.scooby
package exporter

import exporter.DummyExporter.{CsvCountStrategy, Exporter, ListCountStrategy}

import io.cucumber.datatable.DataTable
import io.cucumber.scala.{EN, ScalaDsl}
import org.junit.Assert.assertEquals

class StepDefinitions extends ScalaDsl with EN:

  private var exporter: Exporter = new Exporter()

  private var results: List[DataTable] = List()

  private var stringResult = ""

  Given("""I have an Exporter with a {string} strategy""") { (format: String) =>
    exporter = format match
      case "listCount" => new Exporter() with ListCountStrategy()
      case "csvCount" => new Exporter() with CsvCountStrategy()
      case _ => new Exporter()
  }

  And("""the following result""") { (input: DataTable) =>
    exporter = exporter <-- input
  }

  And("""the following result of many""") { (input: DataTable) =>
    results = input :: results
  }

  When("""I try to export it""") { () =>
    stringResult = exporter.exportAsString
  }
  
  Then("""it should return {string}""") { (result: String) =>
    assertEquals(result, stringResult)
  }

  Then("""it should return first {string} and then {string}""") { (result1: String, result2: String) =>
    exporter = exporter <-- results.tail.head
    assertEquals(result1, exporter.exportAsString)
    exporter = exporter <-- results.head
    assertEquals(result2, exporter.exportAsString)
  }

