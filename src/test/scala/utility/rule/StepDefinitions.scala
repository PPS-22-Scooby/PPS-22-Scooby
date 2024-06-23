package org.unibo.scooby
package utility.rule

import io.cucumber.scala.{EN, ScalaDsl}
import utility.rule.Document.{Document, Link}

import Rule.{given , *}

class StepDefinitions extends ScalaDsl with EN:

  var rule1: Base[Document, Document] = _
  var rule2: Base[Document, Document] = _
  var resultRule: Rule[Document, Document] = _

  var policy1: Policy[Document, Boolean] = _
  var policy2: Policy[Document, Boolean] = _
  var resultPolicy: Rule[Document, Boolean] = _

  Given("""I've two filter rules filter all the explored links, return all links with a certain text""") { () =>

    val link1 = Link("Link1", href="http://www.example.it")
    val link2 = Link("Link2", href="http://www.example2.it")
    val link3 = Link("Link3", href="http://www.example3.it")
    val link4 = Link("Link4", href="http://www.example4.it")

    rule1 = Base[Document, Document](doc => doc.notIn(Seq(link4, link1)))
    rule2 = Base[Document, Document](doc => doc.selectLinkByText("Link1"))
  }

  When("""I join them together""") { () =>
    resultRule = rule1 >> rule2
  }

  Then("""I obtain a result rule unexplored links with a certain text that's the {string} of the two""") { (string: String) =>
    val document = Document(
      Seq(
        Link("Link1", href="http://www.example.it"),
        Link("Link2", href="http://www.example2.it"),
        Link("Link3", href="http://www.example3.it"),
        Link("Link4", href="http://www.example4.it")
      ),
      Seq.empty
    )

    val resultingDocument = resultRule executeOn document
    println(resultingDocument)
  }

  Given("""I've two policy rules document has a tag with a certain id, document has a keyword on a specific path""") { () =>
    policy1 = policy((doc: Document) => doc.tags.exists(tag => tag.id == "tag1"))
    policy2 = Policy[Document, Boolean](doc => false)
  }

  When("""I join them together""") { () =>
    resultPolicy = policy1 and policy2
  }

  Then("""I obtain a result rule document with a tag with a certain id and that present a keyword in a path that's the {string} of the two""") { (string: String) =>
    // Here you should assert that the resultPolicy meets the expected behavior
  }