package org.unibo.scooby
package utility.http

import utility.http.HttpErrorType.GENERIC

enum HttpErrorType:
  case GENERIC
  case DESERIALIZING
  case NETWORK


/**
 * Utility class that represents a HTTP error.
 * @param ex exception representing the error
 * @param errorType type of the error
 */
sealed case class HttpError(ex: Exception, errorType: HttpErrorType):
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
   * @param errorType error type
   * @tparam T type of the object: can be [[String]] or [[Exception]]
   * @return a [[HttpError]] built from the input parameter
   */
  def apply[T >: String | Exception](x: T, errorType: HttpErrorType = GENERIC): HttpError = x match
    case s: String => new HttpError(new Exception(s), errorType)
    case e: Exception => new HttpError(e, errorType)
    case error: HttpError => new HttpError(error.ex, errorType)
    case _ => new HttpError(new Exception("Something failed instantiating error"), errorType)
