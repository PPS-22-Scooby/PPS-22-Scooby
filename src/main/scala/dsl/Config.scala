package org.unibo.scooby
package dsl

import dsl.DSL.ConfigurationBuilder
import utility.http.ClientConfiguration

import monocle.syntax.all.*

import scala.annotation.targetName
import scala.concurrent.duration.FiniteDuration

object Config:

  case class ConfigContext(
                            var options: ConfigOptions,
                            var clientConfiguration: ClientConfiguration
                          )

  case class ConfigOptions(
                            maxDepth: Int = 3,
                            maxLinks: Int = 200
                          )

  case class NetworkConfigurationContext(var config: ClientConfiguration)


  trait PropertyBuilder[R]:
    @targetName("setValue")
    infix def -->(propertyValue: R)(using builder: NetworkConfigurationContext): Unit =
      builder.config = modify(builder.config, propertyValue)

    def modify(previousConfig: ClientConfiguration, value: R): ClientConfiguration


  def config[T](init: ConfigContext ?=> Unit)(using builder: ConfigurationBuilder[T]): Unit =
    given context: ConfigContext = ConfigContext(ConfigOptions(), ClientConfiguration.default)
    init
    builder.configuration = builder.configuration.focus(_.crawlerConfiguration.networkOptions)
      .replace(context.clientConfiguration)


  def Timeout: PropertyBuilder[FiniteDuration] =
    (previousConfig, value) => previousConfig.focus(_.networkTimeout).replace(value)

  def MaxRequests: PropertyBuilder[Int] =
    (previousConfig, value) => previousConfig.focus(_.maxRequests).replace(value)


  object NetworkConfiguration:
    def network(init: NetworkConfigurationContext ?=> Unit)(using context: ConfigContext): Unit =
      given builder: NetworkConfigurationContext = NetworkConfigurationContext(ClientConfiguration.default)
      init
      context.clientConfiguration = builder.config



  object CrawlerGlobalConfiguration:
    def option(init: ConfigOptions ?=> Unit)(using context: ConfigContext): Unit = println()

