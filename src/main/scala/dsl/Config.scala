package org.unibo.scooby
package dsl
import utility.http.Configuration.{ClientConfiguration, Property}
import utility.http.Configuration

import DSL.ConfigurationBuilder
import monocle.syntax.all.*

import scala.concurrent.duration.FiniteDuration

object Config:

  case class ConfigContext(
                            options: ConfigOptions,
                            clientConfiguration: ClientConfiguration
                          )

  case class ConfigOptions(
                            maxDepth: Int = 3,
                            maxLinks: Int = 200
                          )

  case class ClientConfigurationBuilder(var config: ClientConfiguration)


  trait PropertyBuilder[T <: Property[R], R]:
    infix def ->(propertyValue: R)(using builder: ClientConfigurationBuilder): Unit =
      builder.config = builder.config.focus(_.properties).modify(_ :+ instantiateProperty(propertyValue))
      
    def instantiateProperty(value: R): T 



  def config[T](init: ConfigContext ?=> Unit)(using builder: ConfigurationBuilder[T]): Unit =
    given context: ConfigContext = ConfigContext(ConfigOptions(), Configuration.default)
    init
    builder.configuration = builder.configuration.focus(_.crawlerConfiguration.networkOptions)
      .replace(context.clientConfiguration)


  val Timeout: PropertyBuilder[Property.NetworkTimeout, FiniteDuration] = 
    (value: FiniteDuration) => Property.NetworkTimeout(value)
    
  object NetworkConfiguration:
    def network(init: ClientConfiguration ?=> Unit)(using context: ConfigContext): Unit = ???



  object CrawlerGlobalConfiguration:
    def option(init: ConfigOptions ?=> Unit)(using context: ConfigContext): Unit = ???

