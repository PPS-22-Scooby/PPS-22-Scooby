package org.unibo.scooby
package dsl

import dsl.DSL.ConfigurationBuilder
import utility.http.ClientConfiguration

import monocle.Lens
import monocle.macros.GenLens
import monocle.syntax.all.*

import scala.annotation.targetName
import scala.concurrent.duration.FiniteDuration

object Config:
  export PropertyBuilder.*
  export NetworkConfiguration.*
  export CrawlerGlobalConfiguration.*

  case class ConfigContext(
                            var options: ConfigOptions,
                            var clientConfiguration: ClientConfiguration
                          )

  case class ConfigOptions(
                            maxDepth: Int = 3,
                            maxLinks: Int = 200
                          )

  case class NetworkConfigurationContext(var config: ClientConfiguration)

  private type AccessProperty[V] = Lens[ClientConfiguration, V]
  
  enum PropertyBuilder[V](property: AccessProperty[V]):

    case Timeout extends PropertyBuilder[FiniteDuration](GenLens[ClientConfiguration](_.networkTimeout))
    case MaxRequests extends PropertyBuilder[Int](GenLens[ClientConfiguration](_.maxRequests))

    @targetName("setValue")
    infix def -->(propertyValue: V)(using builder: NetworkConfigurationContext): Unit =
      builder.config = property.replace(propertyValue)(builder.config)


  def config[T](init: ConfigContext ?=> Unit)(using builder: ConfigurationBuilder[T]): Unit =
    given context: ConfigContext = ConfigContext(ConfigOptions(), ClientConfiguration.default)
    init
    builder.configuration = builder.configuration.focus(_.crawlerConfiguration.networkOptions)
      .replace(context.clientConfiguration)

  object NetworkConfiguration:
    def network(init: NetworkConfigurationContext ?=> Unit)(using context: ConfigContext): Unit =
      given builder: NetworkConfigurationContext = NetworkConfigurationContext(ClientConfiguration.default)
      init
      context.clientConfiguration = builder.config



  object CrawlerGlobalConfiguration:
    def option(init: ConfigOptions ?=> Unit)(using context: ConfigContext): Unit = println()

