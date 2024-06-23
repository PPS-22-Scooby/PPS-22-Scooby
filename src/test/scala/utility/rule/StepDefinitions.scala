package org.unibo.scooby
package utility.rule


import io.cucumber.scala.{EN, ScalaDsl}
import utility.rule.integration.Document
import Rule.{_, *}


class StepDefinitions extends ScalaDsl with EN:
  opaque type FilterDocumentRule = Rule[Document, Document]
  opaque type PolicyDocumentRule = Rule[Document, Boolean]
  
  val rule1: Option[FilterDocumentRule] = None
  val rule2: Option[FilterDocumentRule] = None
  
  Given("""I've two filter rules filter all the explored links, return all links with a certain text""") :
    () => throw new io.cucumber.scala.PendingException()

  When("""I join them togheter""") :
    () => throw new io.cucumber.scala.PendingException()

  Then("""I obtain a result rule unexplored links with a certain text that's the {string} of the two""") :
    (string: String) => throw new io.cucumber.scala.PendingException()

  Given("""I've two policy rules document has a tag with a certain id, document has a keyword on a specific path"""):
    () => throw new io.cucumber.scala.PendingException()

  When("""I join them togheter""") :
    () => throw new io.cucumber.scala.PendingException()

  Then("""I obtain a result rule document with a tag with a certain id and that present a keyword in a path that's the {string} of the two"""):
    (string: String) => throw new io.cucumber.scala.PendingException()






