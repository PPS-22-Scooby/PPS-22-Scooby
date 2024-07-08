package org.unibo.scooby
package core.scraper

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import utility.document.{RegExpExplorer, ScrapeDocument}
import utility.http.URL

class ScraperActorTest extends TestKit(ActorSystem("ScraperSpec"))
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ImplicitSender:

  val classSelector: Seq[String] = Seq("testClass1", "testClass2")
  val idSelector: Seq[String] = Seq("testId1", "testId2")
  val cssSelector: Seq[String] = classSelector.map(".".concat(_))
  val tagSelector: Seq[String] = Seq("li", "p")
  val regEx: String = "testRegex"
  
  val content: String =
    s"""
      |<html lang="en">
      |<head>
      |  <title>Basic HTML Document</title>
      |</head>
      |<body>
      |  <header>
      |    <h1>Welcome to My Website</h1>
      |  </header>
      |  <nav>
      |    <ul>
      |      ${classSelector.zipWithIndex.map { case (cls, index) =>
                s"""<li class="$cls"><a href="#${index}">Link $index${if index % 2 == 0 then " " + regEx else ""}</a></li>"""
              }.mkString("\n")}
      |      <li><a href="#about">About</a></li>
      |    </ul>
      |  </nav>
      |  <main>
      |    ${idSelector.zipWithIndex.map { case (id, index) =>
              s"""<section id="$id"><h${index+1}>$id</h2><p>This is the $id section${if index % 2 == 0 then " " + regEx else ""}.</p></li>"""
            }.mkString("\n")}
      |    <section id="contact">
      |      <h2>Contact</h2>
      |      <p>This is the contact section.</p>
      |    </section>
      |  </main>
      |  <footer>
      |    <p>&copy; 2024 My Website</p>
      |  </footer>
      |</body>
      |</html>
      |""".stripMargin
  val document: ScrapeDocument = ScrapeDocument(content, URL.empty)
  val regExpDocument: ScrapeDocument with RegExpExplorer = new ScrapeDocument(content, URL.empty) with RegExpExplorer

  val scraperId: ActorRef = TestActorRef(new ScraperActor(ScraperActor.scraperRule(idSelector, "id")))
  val scraperTag: ActorRef = TestActorRef(new ScraperActor(ScraperActor.scraperRule(tagSelector, "tag")))
  val scraperClass: ActorRef = TestActorRef(new ScraperActor(ScraperActor.scraperRule(classSelector, "class")))
  val scraperCss: ActorRef = TestActorRef(new ScraperActor(ScraperActor.scraperRule(cssSelector, "css")))
  val scraperRegEx: ActorRef = TestActorRef(new ScraperActor(ScraperActor.regexSelectorsRule(Seq(regEx))))

  override def beforeAll(): Unit =
    val system = ActorSystem("ScraperTestSystem")

  "Scraper actor" should:
    "process Messages.Scrape message correctly" in:

      scraperId ! ScraperActor.Messages.Scrape(document)
      scraperTag ! ScraperActor.Messages.Scrape(document)
      scraperClass ! ScraperActor.Messages.Scrape(document)
      scraperCss ! ScraperActor.Messages.Scrape(document)

      scraperId ! ScraperActor.Messages.Scrape(document)
      scraperTag ! ScraperActor.Messages.Scrape(document)
      scraperClass ! ScraperActor.Messages.Scrape(document)
      scraperCss ! ScraperActor.Messages.Scrape(document)

    "process a document and send the result to sender" in:

      // TestProbe to intercept messages
      val probeId = TestProbe()
      val probeTag = TestProbe()
      val probeClass = TestProbe()
      val probeCss = TestProbe()
      val probeRegEx = TestProbe()

      scraperId.tell(ScraperActor.Messages.Scrape(document), probeId.ref)
      scraperTag.tell(ScraperActor.Messages.Scrape(document), probeTag.ref)
      scraperClass.tell(ScraperActor.Messages.Scrape(document), probeClass.ref)
      scraperCss.tell(ScraperActor.Messages.Scrape(document), probeCss.ref)
      scraperRegEx.tell(ScraperActor.Messages.Scrape(regExpDocument), probeRegEx.ref)

      val expectedById = idSelector.map(document.getElementById).map(_.text).map(elem => Result(elem))
        .reduceOption((res1, res2) => res1.aggregate(res2)).getOrElse(Result.empty[String])
      val expectedByTag = tagSelector.flatMap(document.getElementByTag).map(_.text).map(elem => Result(elem))
        .reduceOption((res1, res2) => res1.aggregate(res2)).getOrElse(Result.empty[String])
      val expectedByClass = classSelector.flatMap(document.getElementByClass).map(_.text).map(elem => Result(elem))
        .reduceOption((res1, res2) => res1.aggregate(res2)).getOrElse(Result.empty[String])
      val expectedByCss = cssSelector.flatMap(sel => document.select(sel)).map(_.text).map(elem => Result(elem))
        .reduceOption((res1, res2) => res1.aggregate(res2)).getOrElse(Result.empty[String])
      val expectedByRegEx = Result(regExpDocument.find(regEx))

      probeId.expectMsg(ScraperActor.Messages.SendPartialResult(expectedById))
      probeTag.expectMsg(ScraperActor.Messages.SendPartialResult(expectedByTag))
      probeClass.expectMsg(ScraperActor.Messages.SendPartialResult(expectedByClass))
      probeCss.expectMsg(ScraperActor.Messages.SendPartialResult(expectedByCss))
      probeRegEx.expectMsg(ScraperActor.Messages.SendPartialResult(expectedByRegEx))

  // Cleanup resources after all tests
  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)
