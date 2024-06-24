package org.unibo.scooby
package utility.rule

import io.cucumber.scala.{EN, ScalaDsl}
import utility.rule.Document.{Document, Link}

import Rule.{given , *}

class StepDefinitions extends ScalaDsl with EN:

  var rule1: Base[Document, Document] = _
  var rule2: Base[Document, Document] = _
  var resultRule: Rule[Document, Document] = _

  var policy1: Policy[Document] = _
  var policy2: Policy[Document] = _
  var resultPolicy: Rule[Document, Boolean] = _

  Given("""I've two filter rules filter all the explored links, return all links with a certain text""") { () =>

    throw new io.cucumber.scala.PendingException()
  }

  When("""I join them together""") { () =>
    throw new io.cucumber.scala.PendingException()
  }

  Then("""I obtain a result rule unexplored links with a certain text that's the {string} of the two""") { (string: String) =>
    throw new io.cucumber.scala.PendingException()
  }

  Given("""I've two policy rules document has a tag with a certain id, document has a keyword on a specific path""") { () =>
    throw new io.cucumber.scala.PendingException()
  }

  When("""I join them together""") { () =>
    throw new io.cucumber.scala.PendingException()
  }

  Then("""I obtain a result rule document with a tag with a certain id and that present a keyword in a path that's the {string} of the two""") { (string: String) =>
    throw new io.cucumber.scala.PendingException()
  }