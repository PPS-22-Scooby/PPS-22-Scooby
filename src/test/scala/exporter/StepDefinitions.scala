package org.unibo.scooby
package exporter

import io.cucumber.datatable.DataTable
import io.cucumber.scala.{EN, ScalaDsl}

class StepDefinitions extends ScalaDsl with EN:


  Given("""^I have an Exporter""") { () =>
    println("I have no content to export")
  }

  And("""^an empty result""") { () =>
    println("I have an empty result")
  }

  Given("""^I have an Exporter with a csv strategy""") { () =>
    println("I have an Csv exporter")
  }

  And("""^the following result""") { (result: DataTable) =>
    println(result)
  }

  When("""^I try to export it""") { () =>
    println("Exporting it")
  }
  
  Then("""it should return {string}""") { (result: String) => {
    println(result)
  }}