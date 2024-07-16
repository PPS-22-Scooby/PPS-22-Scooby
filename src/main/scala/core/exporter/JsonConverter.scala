package org.unibo.scooby
package core.exporter

import utility.document.html.HTMLElement
import play.api.libs.json.*
import play.api.libs.functional.syntax._
import play.api.libs.json.Writes._

/**
 * Provides functionality to convert `HTMLElement` instances to JSON.
 *
 * This object defines an implicit `Writes` instance for the `HTMLElement` class,
 * enabling the Play Framework's JSON library to serialize `HTMLElement` objects.
 * The serialization includes the element's tag, attributes, optional text content,
 * and children elements recursively.
 */
object JsonConverter:

  // Defines how to write an `HTMLElement` to JSON.
  // The `Writes` instance specifies how to convert the various fields of an `HTMLElement`
  // into a JSON representation. This includes the tag name, attributes map, optional text content,
  // and children elements. The children elements are handled recursively using `lazyWrite`
  // to support nested structures without running into issues with infinite recursion.
  given htmlElementWrites: Writes[HTMLElement] = (
    (JsPath \ "tag").write[String] and
      (JsPath \ "attributes").write[Map[String, String]] and
      (JsPath \ "text").writeNullable[String] and
      (JsPath \ "children").lazyWrite(Writes.seq[HTMLElement](htmlElementWrites))
    ) (html => (html.tag, html.attributes, if html.text.isEmpty then None else Some(html.text), html.children))
