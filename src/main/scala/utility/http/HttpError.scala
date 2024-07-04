package org.unibo.scooby
package utility.http

sealed case class HttpError(ex: Exception):
  def message: String = ex.getMessage

extension (x: String)
  def asHttpError: HttpError = HttpError(x)

extension (x: Exception)
  def asHttpError: HttpError = HttpError(x)

object HttpError:
  def apply[T >: String | Exception](x: T): HttpError = x match
    case s: String => new HttpError(new Exception(s))
    case e: Exception => new HttpError(e)
    case error: HttpError => new HttpError(error.ex)


