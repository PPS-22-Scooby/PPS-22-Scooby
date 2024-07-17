package org.unibo.scooby
package dsl
import org.w3c.dom.html.HTMLHtmlElement

trait Scooby:
  def run(): Unit

  def setConfiguration(config: Configuration): Unit

trait CrawlDocument:
  def links: Seq[String]

trait CrawlDocumentContext:
  def document: CrawlDocument

trait HTMLElementContext:
  def element: HTMLElement

trait HTMLElement:
  def tag: String

  def id: String

  def classes: Seq[String]

  def attribute(attr: String): String

  def parent: HTMLElement

  def children: Seq[HTMLElement]

  def outerHTML: String

trait ScrapeDocument:
  def getElementsById(id: String): Seq[HTMLElement]

  def elements: Seq[HTMLElement]

trait ScrapeDocumentContext:
  def document: ScrapeDocument

// case class Configuration(options: Set[OptionGroup])

case class ConfigurationBuilder(options: scala.collection.mutable.Set[OptionGroup]):
  def build: Configuration = ???

enum CrawlingStrategy:
  case Depth_First

enum OptionType:
  case NetworkTimeout
  case ExternalDomains
  case MaxDepth
  case Strategy

  def ->(other: Any)(using builder: ConfigurationBuilder): Option = ???

enum OptionCategory:
  case NETWORK
  case CRAWLING

case class Option(optType: OptionType, value: AnyVal)

trait Rule[A, B]:
  def &&(other: Rule[A, B]): Rule[A, B]

  def ||(other: Rule[A, B]): Rule[A, B]

case class OptionGroup(category: OptionCategory, options: scala.collection.mutable.Set[Option])

def scooby(init: Scooby ?=> Unit) =
  given scooby: Scooby = ???

  init
  scooby.run()

def config(defineConfig: ConfigurationBuilder ?=> Unit)(using scooby: Scooby) =
  given configuration: ConfigurationBuilder = ???
  defineConfig
  scooby.setConfiguration(configuration.build)

def network(defineNetworkConfig: OptionGroup ?=> Unit)(using builder: ConfigurationBuilder) =
  given group: OptionGroup = OptionGroup(OptionCategory.NETWORK, scala.collection.mutable.Set.empty)

  defineNetworkConfig
  builder.options + group

def crawling(defineCrawlingConfig: OptionGroup ?=> Unit)(using builder: ConfigurationBuilder) =
  given group: OptionGroup = OptionGroup(OptionCategory.CRAWLING, scala.collection.mutable.Set.empty)

  defineCrawlingConfig
  builder.options + group

def crawl(defineLinks: CrawlDocumentContext ?=> Seq[String])(using scooby: Scooby): Unit = ???

def scrape[T](defineScraping: ScrapeDocumentContext ?=> Seq[T])(using scooby: Scooby): Unit = ???

def elements(using context: ScrapeDocumentContext): Seq[HTMLElement] = context.document.elements

def matchesOf(regex: String)(using context: ScrapeDocumentContext): Seq[String] = ???

def links(using context: CrawlDocumentContext): Seq[String] = context.document.links

extension [T](elements: Seq[T])
  infix def thatFollowRule(rule: => Rule[T, Boolean]): Seq[T] = ???

trait Writer[T]:
  def write(that: T): String

def write[T](that: T)(using writer: Writer[T]): Unit = writer.write(that)

extension [T: Writer](result: Seq[T])
  def asJson[A](headers: String*): Seq[A] = ???

extension [T](result: Seq[T])
  def asCsv[A](headers: String*): Seq[A] = ???

trait ResultContext[T]:
  def results: Seq[T]

extension [T](results: Seq[T])
  infix def exportAs[A](defineExport: ResultContext[T] ?=> Seq[A])(using scooby: Scooby): Seq[A] = ???


def results[T](using ResultContext[T]): Seq[T] = ???

def rule(function: HTMLElementContext ?=> Boolean): Rule[HTMLElement, Boolean] = ???

def element(using context: HTMLElementContext): HTMLElement = ???

extension (elements: Seq[HTMLElement])
  infix def withId(id: String): Seq[HTMLElement] = elements.filter(_.id == id)

  infix def withClass(cssClass: String): Seq[HTMLElement] = elements.filter(_.classes.contains(cssClass))

  infix def withTag(tag: String): Seq[HTMLElement] = elements.filter(_.tag == tag)


def hasTag(tag: String): Rule[HTMLElement, Boolean] = ???
def hasClass(cssClass: String): Rule[HTMLElement, Boolean] = ???
def hasId(id: String): Rule[HTMLElement, Boolean] = ???

object Test:

  import OptionType.*
  import scala.concurrent.duration._

  given pairWriter: Writer[(String, Int)] = ??? // in teoria ce lo dà la libreria

  given stringPairWriter: Writer[(String, String)] = ??? // in teoria ce lo dà la libreria

  import CrawlingStrategy.*

  def main() =
    // ESEMPIO 1
    // =====================================================================
    scooby:
      config:
        network:
          NetworkTimeout -> 5.seconds
        crawling:
          MaxDepth -> 3
          ExternalDomains -> false
          Strategy -> Depth_First

      crawl:
        links filter (_.endsWith("example"))

      scrape:
        elements thatFollowRule {

          hasTag("div") &&
            hasId("toSearch") &&
            rule {
              element.classes contains ("gorgeous")
            } &&
            rule {
              element.parent.tag == "div"
            }

        } exportAs :

          results.groupBy(_.tag)
            .map { case (element, occurrences) => (element, occurrences.size) }
            .toSeq
            .asJson("tag", "occurrences")


    // INPUT EXPORTER :   Seq(HTMLElement("div"...), HTMLElement("div"...), HTMLElement("div"...), HTMLElement("a"...))
    // toSeq          :    => Seq(("div", 3), ("a", 1))
    // asJson         :    => "[{"tag": "div", "occurences": 3}, {"tag": "a", "occurrences": 1}]"

    // ESEMPIO 2
    // =====================================================================
    scooby:
      crawl:
        links

      scrape:
        matchesOf("<div[^>]*>") exportAs results

    // ESEMPIO 3
    // =====================================================================
    scooby:
      crawl:
        links filter (_.contains("/intermediatePath/"))

      scrape:
        elements withClass "gorgeous" withTag "div" exportAs :
          results
            .map(element => (element.id, element.children(0).outerHTML))
            .asJson("id", "firstChild")


    // ESEMPIO 4
    // =====================================================================
    scooby:
      crawl:
        links

      scrape:
        matchesOf("""class="gorgeous"""") ++ (elements withClass "gorgeous") exportAs :

          results.map(_.toString).asCsv()
