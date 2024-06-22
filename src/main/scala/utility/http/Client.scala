package org.unibo.scooby
package utility.http

import utility.http.HttpMethod.*


sealed case class ClientBackend()
sealed case class ClientFrontend()

sealed case class HttpClient():
  def send(request: Request): Response = ???

trait ResponseDeserializer[T]:
  def deserialize(response: Response): T

object ResponseDeserializer:
  given default: ResponseDeserializer[Response] = (response: Response) => response