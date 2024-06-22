package org.unibo.scooby
package utility.http

trait HttpClient

object Clients:
  import utility.http.Backends.SttpBackend

  class SimpleHttpClient extends HttpClient with SttpBackend


trait Deserializer[R, T]:
  def deserialize(response: R): T

object Deserializer:
  given default: Deserializer[Response, Response] = (response: Response) => response
  given string: Deserializer[Response, String] = (response: Response) => response.body.getOrElse("")