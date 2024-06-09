package org.unibo.scooby
package exporter

import io.cucumber.scala.{EN, ScalaDsl}

class StepDefinitions extends ScalaDsl with EN:

  Given("""^I have no content to export""") { () =>
    println("I have no content to export")
  }

  When("""^I try to export it""") { () =>
    println("Exporting it")
  }

  Then("""^it should return an empty list""") { () =>
    println("Returning an empty list")
  }