package org.unibo.scooby
package core.exporter

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element, Node, TextNode}
import org.jsoup.select.NodeVisitor
import play.api.libs.json._

object JsonConverter:

  def toJson(html: String): JsValue =
    val document = Jsoup.parseBodyFragment(html)
    elementToJson(document.body())

  def toJsonString(html: String): String =
    Json.prettyPrint(toJson(html))

  private def elementToJson(element: Element): JsValue =
    val children = element.childNodes().toArray.collect {
      case e: Element =>
        elementToJson(e)
      case t: TextNode if t.text().trim.nonEmpty =>
        JsString(t.text().trim)
    }

    Json.obj(
      "tag" -> element.tagName(),
      "attributes" -> attributesToJson(element),
      "children" -> Json.toJson(children)
    )

  private def attributesToJson(element: Element): JsValue =
    val attrs = element.attributes().asList().toArray.map { attr =>
      val attribute = attr.asInstanceOf[org.jsoup.nodes.Attribute]
      attribute.getKey -> JsString(attribute.getValue)
    }.toMap

    Json.toJson(attrs)