package org.unibo.scooby
package utility.http

import utility.http.Configuration.ClientConfiguration

import scala.concurrent.duration.Duration

/**
 * Empty trait that represents an HTTP client. To be useful, you need to mix-in [[Backend]] Traits
 */
trait HttpClient(configuration: ClientConfiguration)

object Clients:
  import utility.http.Backends.SttpBackend

  /**
   * Simple HTTP client used for synchronous HTTP calls with the sttp library as backend
   */
  class SimpleHttpClient(configuration: ClientConfiguration = Configuration.default)
      extends HttpClient(configuration) with SttpBackend

type Client[R] = HttpClient & Backend[R]

object Configuration:
  private type ConfigurationEntry = (ConfigurationEntryType, Any)

  enum ConfigurationEntryType(val valueType: Class[?]):
    case NETWORK_TIMEOUT extends ConfigurationEntryType(classOf[Duration])

  case class ClientConfiguration(configs: ConfigurationEntry*)

  object ClientConfiguration:
    def apply(configs: ConfigurationEntry*): Either[HttpError, ClientConfiguration] =
      configs.find {
        case (entryType: ConfigurationEntryType, value: Any) => value.getClass.isAssignableFrom(entryType.valueType)
      } match
        case Some((entryType: ConfigurationEntryType, entryValue: Any)) =>
          Left((s"$entryValue doesn't satisfy the type of configuration entry: $entryType which " +
            s"is ${entryType.valueType}").asHttpError)
        case None => Right(new ClientConfiguration(configs*))

  import ConfigurationEntryType.*
  import scala.concurrent.duration.DurationInt

  def default: ClientConfiguration = ClientConfiguration(NETWORK_TIMEOUT -> 5.seconds)
    .getOrElse(new ClientConfiguration())
