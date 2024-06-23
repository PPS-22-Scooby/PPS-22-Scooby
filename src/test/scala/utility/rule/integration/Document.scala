package org.unibo.scooby
package utility.rule.integration

case class Link(text: String, href: String)

case class Document(links: Seq[Link]):
  def selectLinkByText(text: String): Document =
    Document(links.filterNot:
      case Link(t, _) => text != t
    )

  def filterLinksByLink(href: String): Document =
    Document(links.filterNot:
      case Link(_, h) => href != h
    )

  def notIn(seq: Seq[Link]): Document =
    Document(links.filterNot(seq.contains(_)))


object TestDocument extends App:
  val link1 = Link("Link1", href="http://www.example.it")
  val link2 = Link("Link2", href="http://www.example2.it")
  val link3 = Link("Link3", href="http://www.example3.it")
  val link4 = Link("Link4", href="http://www.example4.it")


  println(Document(Seq(link1, link2, link3, link4)).notIn(Seq(link4, link1)))
