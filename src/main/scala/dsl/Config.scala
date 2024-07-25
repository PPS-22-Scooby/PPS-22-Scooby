package org.unibo.scooby
package dsl

import dsl.DSL.ConfigurationBuilder
import utility.http.ClientConfiguration

import monocle.Lens
import monocle.macros.GenLens
import monocle.syntax.all.*

import scala.concurrent.duration.FiniteDuration

object Config:
  export ConfigOps.SafeOps.*
  export ConfigOps.PropertyBuilder.*
  import ConfigContexts.*

  object ConfigOps:
    import dsl.syntax.catchRecursiveCtx

    object SafeOps:
      import UnsafeOps.* 

      inline def config[T](init: ConfigContext ?=> Unit)(using builder: ConfigurationBuilder[T]): Unit =
        catchRecursiveCtx[ConfigContext]("config")
        configOp(init)

      inline def headers(init: HeadersContext ?=> Unit)(using context: NetworkConfigurationContext): Unit =
        catchRecursiveCtx[HeadersContext]("headers")
        headersOp(init)

      inline def network(init: NetworkConfigurationContext ?=> Unit)(using context: ConfigContext): Unit =
        catchRecursiveCtx[NetworkConfigurationContext]("network")
        networkOp(init)

      inline def option(init: OptionConfigurationContext ?=> Unit)(using context: ConfigContext): Unit =
        catchRecursiveCtx[OptionConfigurationContext]("option")
        optionOp(init)

      extension(x: String)
        infix def to(value: String)(using context: HeadersContext): Unit = context.headers =
          context.headers + (x -> value)


    private[Config] object UnsafeOps:
      def configOp[T](init: ConfigContext ?=> Unit)(using builder: ConfigurationBuilder[T]): Unit =
        given context: ConfigContext = ConfigContext(ConfigOptions(), ClientConfiguration.default)
        init
        builder.configuration = builder.configuration
          .focus(_.crawlerConfiguration.networkOptions)     .replace(context.clientConfiguration)
          .focus(_.crawlerConfiguration.maxDepth)           .replace(context.options.maxDepth)
          .focus(_.coordinatorConfiguration.maxLinks)       .replace(context.options.maxLinks)

      def headersOp(init: HeadersContext ?=> Unit)(using context: NetworkConfigurationContext): Unit =
          given builder: HeadersContext = HeadersContext(Map.empty)
          init
          context.config = context.config.focus(_.headers).replace(builder.headers)

      def networkOp(init: NetworkConfigurationContext ?=> Unit)(using context: ConfigContext): Unit =
          given builder: NetworkConfigurationContext = NetworkConfigurationContext(ClientConfiguration.default)
          init
          context.clientConfiguration = builder.config

      def optionOp(init: OptionConfigurationContext ?=> Unit)(using context: ConfigContext): Unit =
          given builder: OptionConfigurationContext = OptionConfigurationContext(ConfigOptions())
          init
          context.options = builder.config


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

    

  private[Config] object ConfigContexts:

    case class ConfigContext(
                              var options: ConfigOptions,
                              var clientConfiguration: ClientConfiguration
                            )

    case class HeadersContext(
                            var headers: Map[String, String]
                            )

    case class ConfigOptions(maxDepth: Int = 3, maxLinks: Int = 200)

    trait ConfigurationContext[T]:
      var config: T

    case class NetworkConfigurationContext(var config: ClientConfiguration)
      extends ConfigurationContext[ClientConfiguration]
    case class OptionConfigurationContext(var config: ConfigOptions)
      extends ConfigurationContext[ConfigOptions]

