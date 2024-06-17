//package org.unibo.scooby
//package scraper
//
//import scraper.DummyScraper.{AttributeStrategy, RegexStrategy, Scraper, SelectorsStrategy}
//
//import io.cucumber.scala.{EN, ScalaDsl}
//import org.junit.Assert.*
//
//import scala.util.matching.Regex
//
//class StepDefinitions extends ScalaDsl with EN:
//
//  private var scraper: Scraper = new Scraper()
//
//  private var result: String = ""
//
//  private var stringResult = ""
//
//  private val doc: String =
//    """
//      |<html>
//      | <head>
//      |   <title>Sample Title</title>
//      | </head>
//      | <body>
//      |   <div id="content" class="main" data-info="example">
//      |     <p>Hello, World!</p>
//      |     <a>Hello, World! That's a prova</a>
//      |   </div>
//      |   <div id="content" class="main md" data-info="example">
//      |     <p>Hello, World!</p>
//      |     <a>Hello, World! That's a prova</a>
//      |   </div>
//      | </body>
//      |</html>
//      |""".stripMargin
//
//  Given("""I have a Scraper with a {string} filter rule""") { (format: String) =>
//    scraper = format match
//      case "selectors" => new Scraper() with SelectorsStrategy(selectors)
//      case "regex" => new Scraper() with RegexStrategy(regex)
//      case "attribute" => new Scraper() with AttributeStrategy(attributes)
//      case _ => new Scraper()
//  }
//
//  When("""^I run the tests$"""): () =>
//    // Implementazione del passo "When"
//    println("Running the tests")
//
//  Then("""^they should pass$"""): () =>
//    // Implementazione del passo "Then"
//    println("Tests passed")
//    assertTrue(true)
