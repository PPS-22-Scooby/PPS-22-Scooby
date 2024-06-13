package org.unibo.scooby
package crawler

import io.cucumber.scala.{EN, ScalaDsl}

class StepDefinitions extends ScalaDsl with EN:

  Given("""a non well-formatted url"""): () =>
    throw new io.cucumber.scala.PendingException()

  When("""a crawler tries to check it for data"""): () =>
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.scala.PendingException()

  Then(
    """will notice the user that the url can't be parsed and continues with other urls"""
  ): () =>
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.scala.PendingException()

  Given("""an URL of an offline website""") : () =>
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.scala.PendingException()

  When("""a crawler tries to fetch data from it""") : () =>
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.scala.PendingException()

  When("""reach a connection timeout""") : () =>
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.scala.PendingException()

  Given("""a user Pino that navigate to an url""") : () =>
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.scala.PendingException()

  Given("""the url will return the Content-Type header {string}""") :
    (headerType: String) =>
      // Write code here that turns the phrase above into concrete actions
      throw new io.cucumber.scala.PendingException()

  When("""it will start crawling""") : () =>
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.scala.PendingException()

  Then("""will use the {string} strategy for getting data"""):
    (strategyType: String) =>
      // Write code here that turns the phrase above into concrete actions
      throw new io.cucumber.scala.PendingException()

  Given("""a user Fred that want to crawl the url https:\/\/www.youtube.com\/watch?v=dQw4w9WgXcQ""") : () =>
     // Write code here that turns the phrase above into concrete actions
     throw new io.cucumber.scala.PendingException()

  Given ("""the url will return the Content-Type header video\/webm""") : () =>
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.scala.PendingException()

