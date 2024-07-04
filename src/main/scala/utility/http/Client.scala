package org.unibo.scooby
package utility.http

/**
 * Empty trait that represents an HTTP client. To be useful, you need to mix-in [[Backend]] Traits
 */
trait HttpClient

object Clients:
  import utility.http.Backends.SttpBackend

  /**
   * Simple HTTP client used for synchronous HTTP calls with the sttp library as backend
   */
  class SimpleHttpClient extends HttpClient with SttpBackend

type Client[R] = HttpClient & Backend[R]
