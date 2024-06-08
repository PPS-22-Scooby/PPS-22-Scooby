package org.unibo.scooby
package scraper

import io.cucumber.scala.{EN, ScalaDsl}
import org.junit.Assert._

class StepDefinitions extends ScalaDsl with EN {

  Given("""^I have a configured Cucumber project$""") { () =>
    // Implementazione del passo "Given"
    println("Cucumber project configured")
  }

  When("""^I run the tests$""") { () =>
    // Implementazione del passo "When"
    println("Running the tests")
  }

  Then("""^they should pass$""") { () =>
    // Implementazione del passo "Then"
    println("Tests passed")
    assertTrue(true)
  }
}
