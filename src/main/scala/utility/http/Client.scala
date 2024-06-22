package org.unibo.scooby
package utility.http

import utility.http.HttpMethod.*


sealed case class ClientBackend()
sealed case class ClientFrontend()

sealed case class HttpClient():
  def send(response: Request): Response = ???

trait ResponseDeserializer[T]:
  def deserialize(response: Response): T
