package org.unibo.scooby
package utility.http

/**
 * Utility class that represents a HTTP error.
 * @param ex exception representing the error
 */
sealed case class HttpError(ex: Exception):
  /**
   * Returns a string representation of this error
   * @return a [[String]] representing an error message
   */
  def message: String = ex.getMessage

/**
 * Simple extension to rapidly convert [[String]]s to [[HttpError]]s
 */
extension (x: String)
  /**
   * Converts a [[String]] to [[HttpError]]
   * @return a [[HttpError]] built from this [[String]]
   */
  def asHttpError: HttpError = HttpError(x)

/**
 * Simple extension to rapidly convert [[Exception]]s to [[HttpError]]s
 */
extension (x: Exception)
  /**
   * Converts a [[Exception]] to [[HttpError]]
   * @return a [[HttpError]] encapsulating an [[Exception]]
   */
  def asHttpError: HttpError = HttpError(x)

/**
 * Companion object for [[HttpError]]. [[HttpError]]s must typically be instantiated through the `apply` method.
 */
object HttpError:
  /**
   * Instantiation method for [[HttpError]]s
   * @param x object to be converted to a [[HttpError]]
   * @tparam T type of the object: can be [[String]] or [[Exception]]
   * @return a [[HttpError]] built from the input parameter
   */
  def apply[T >: String | Exception](x: T): HttpError = x match
    case s: String => new HttpError(new Exception(s))
    case e: Exception => new HttpError(e)
    case error: HttpError => new HttpError(error.ex)
    case _ => HttpError("")


