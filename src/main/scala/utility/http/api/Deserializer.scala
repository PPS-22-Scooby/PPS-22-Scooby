package org.unibo.scooby
package utility.http

import utility.document.{CrawlDocument, ScrapeDocument}
import utility.http.HttpErrorType.DESERIALIZING

/**
 * The deserializer is a trait that transforms HTTP responses of class [[R]] in objects of class [[T]]. It can be seen
 * as a utility that makes the user avoid having to access the HTTP response and lets him/her use an already
 * deserialized object instead
 *
 * @tparam R
 *   "form" of the responses used by the backend in hand
 * @tparam T
 *   type of the object to which the responses [[R]] are deserialized
 */
trait Deserializer[R, T]:
  /**
   * Deserializes the response into a [[T]] object
   *
   * @param response
   *   response to be deserialized
   * @return
   *   a [[Either]] with a deserialized object of class [[T]] or with an error otherwise
   */
  def deserialize(response: R): Either[HttpError, T]

/**
 * Collection of useful deserializers
 */
object Deserializer:
  /**
   * Default deserializer, used when the user wants directly the a [[Response]] object (in practice it's as skipping the
   * deserializing phase).
   * @return
   *   a non-deserialized [[Response]] object
   */
  given default: Deserializer[Response, Response] = (response: Response) => Right(response)

  /**
   * Utility deserializer that gives [[String]] s from [[Response]] s. It simply extracts the response's body
   * @return
   *   the [[String]] body of the Response
   */
  given body: Deserializer[Response, String] = (response: Response) =>
    response.body.fold(Left(HttpError("Empty body")))(Right(_))

  /**
   * Utility deserializer that gives [[Option]] of [[String]] from [[Response]]. It simply extracts the response's body
   * (if present)
   * @return
   *   [[Some]] containing the body if present, [[None]] otherwise
   */
  given optionalBody: Deserializer[Response, Option[String]] = (response: Response) => Right(response.body)

  /**
   * Utility function that converts response to its body only if it has the correct "content-type" header
   * @param response Response to be converted
   * @return a [[Either]] containing the body of the [[Response]] if it is a valid document with the required header,
   *         or an [[HttpError]] otherwise
   */
  private def acceptWithContentTypeText(response: Response): Either[HttpError, String] =
    response.headers.get("content-type") match
      case Some(contentType) if contentType.startsWith("text/") =>
        body.deserialize(response)
      case _ =>
        Left(HttpError(s"${response.request.url} does not have a text content type", DESERIALIZING))
  
  /**
   * Utility deserializer that gives [[CrawlDocument]] from [[Response]].
   * @return
   *   a [[CrawlDocument]] built from the body of the [[Response]]
   */
  given crawlDocument: Deserializer[Response, CrawlDocument] =
    (response: Response) => acceptWithContentTypeText(response).map(CrawlDocument(_, response.request.url))

  /**
   * Utility deserializer that gives [[ScrapeDocument]] from [[Response]]
   * @return
   *   a [[ScrapeDocument]] built from the body of the [[Response]]
   */
  given scrapeDocument: Deserializer[Response, ScrapeDocument] =
  (response: Response) => acceptWithContentTypeText(response).map(ScrapeDocument(_, response.request.url))

