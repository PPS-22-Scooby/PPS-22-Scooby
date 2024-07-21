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

  case class ConfigOptions(maxDepth: Int = 3, maxLinks: Int = 200)

  trait ConfigurationContext[T]:
    var config: T

  case class NetworkConfigurationContext(var config: ClientConfiguration)
    extends ConfigurationContext[ClientConfiguration]
  case class OptionConfigurationContext(var config: ConfigOptions)
    extends ConfigurationContext[ConfigOptions]

  private type AccessProperty[T,V] = Lens[T, V]

  enum PropertyBuilder[T, C <: ConfigurationContext[T], V](property: AccessProperty[T,V]):

    case Timeout extends PropertyBuilder[ClientConfiguration, NetworkConfigurationContext, FiniteDuration] (
      GenLens[ClientConfiguration](_.networkTimeout)
    )
    case MaxRequests extends PropertyBuilder[ClientConfiguration, NetworkConfigurationContext, Int] (
      GenLens[ClientConfiguration](_.maxRequests)
    )

    case MaxDepth extends PropertyBuilder[ConfigOptions, OptionConfigurationContext, Int] (
      GenLens[ConfigOptions](_.maxDepth)
    )

    case MaxLinks extends PropertyBuilder[ConfigOptions, OptionConfigurationContext, Int](
      GenLens[ConfigOptions](_.maxLinks)
    )


    infix def is(propertyValue: V)(using builder: C): Unit =
      builder.config = property.replace(propertyValue)(builder.config)



  def config[T](init: ConfigContext ?=> Unit)(using builder: ConfigurationBuilder[T]): Unit =
    given context: ConfigContext = ConfigContext(ConfigOptions(), ClientConfiguration.default)
    init
    builder.configuration = builder.configuration
      .focus(_.crawlerConfiguration.networkOptions)     .replace(context.clientConfiguration)
      .focus(_.crawlerConfiguration.maxDepth)           .replace(context.options.maxDepth)
      .focus(_.coordinatorConfiguration.maxLinks)       .replace(context.options.maxLinks)

  object NetworkConfiguration:
    def network(init: NetworkConfigurationContext ?=> Unit)(using context: ConfigContext): Unit =
      given builder: NetworkConfigurationContext = NetworkConfigurationContext(ClientConfiguration.default)
      init
      context.clientConfiguration = builder.config


  object CrawlerGlobalConfiguration:
    def option(init: OptionConfigurationContext ?=> Unit)(using context: ConfigContext): Unit =
      given builder: OptionConfigurationContext = OptionConfigurationContext(ConfigOptions())
      init
      context.options = builder.config




