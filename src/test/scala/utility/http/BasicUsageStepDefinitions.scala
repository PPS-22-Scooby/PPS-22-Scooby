package org.unibo.scooby
package utility.http

import io.cucumber.scala.{EN, ScalaDsl}

class BasicUsageStepDefinitions extends ScalaDsl with EN:

  Given("""a simple {string} request"""): (requestType: String) =>
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.scala.PendingException()

  Given("""a URL {string}"""): (string: String) =>
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.scala.PendingException()


  When("""i make the HTTP call"""): () =>
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.scala.PendingException()


  Then("""the returned content should be not empty"""): () =>
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.scala.PendingException()

  Then("""it should return an error"""): () =>
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.scala.PendingException()


  Then("""the status code should be {int} and the header Content-Type {string}"""):
    (statusCode: Int, contentType: String) =>
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.scala.PendingException()

