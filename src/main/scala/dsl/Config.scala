package org.unibo.scooby
package dsl

import dsl.DSL.ConfigurationBuilder
import utility.http.ClientConfiguration

import monocle.Lens
import monocle.macros.GenLens
import monocle.syntax.all.*

import scala.concurrent.duration.FiniteDuration

/**
 * Container for all the generic configuration-related DSL keywords.
 */
object Config:
  export ConfigOps.SafeOps.*
  export ConfigOps.PropertyBuilder.*
  import ConfigContexts.*

  /**
   * Collection of DSL operators to customize the generic properties of Scooby application
   *
   * Example usage:
   * {{{
   * config:
   *    network:
   *      Timeout is 5.seconds
   *      headers:
   *        "HeaderName" to "HeaderValue"
   *    options:
   *      MaxDepth is 10
   * }}}
   */
  object ConfigOps:
    import dsl.syntax.catchRecursiveCtx

    /**
     * Type alias representing the section under "config"
     */
    private type GlobalConfigScope = ConfigContext ?=> Unit
    /**
     * Type alias representing the section under "headers"
     */
    private type HeadersConfigScope = HeadersContext ?=> Unit
    /**
     * Type alias representing the section under "network"
     */
    private type NetworkConfigScope = NetworkConfigurationContext ?=> Unit
    /**
     * Type alias representing the section under "option"
     */
    private type OptionConfigScope = OptionConfigurationContext ?=> Unit

    /**
     * Facade for Configuration DSL operators that performs syntax checks.
     */
    object SafeOps:
      import UnsafeOps.*

      /**
       * Top level keyword for defining the generic configuration of Scooby
       * @param block nested definitions of configuration settings
       * @param globalScope global scooby scope (i.e. "scope: ...")
       * @tparam T type of the configuration (i.e. type of the result returned by this scraping behavior)
       */
      inline def config[T](block: GlobalConfigScope)(using globalScope: ConfigurationBuilder[T]): Unit =
        catchRecursiveCtx[ConfigContext]("config")
        configOp(block)

      /**
       * "config" level keyword for setting network configurations
       * @param block nested definitions of network settings
       * @param context context provided by the "config" keyword
       */
      inline def network(block: NetworkConfigScope)(using context: ConfigContext): Unit =
        catchRecursiveCtx[NetworkConfigurationContext]("network")
        networkOp(block)

      /**
       * "network" level keyword for setting the headers of the HTTP requests made within Scooby
       * @param block nested definitions of the headers value
       * @param context context provided by the "network" keyword
       */
      inline def headers(block: HeadersConfigScope)(using context: NetworkConfigurationContext): Unit =
        catchRecursiveCtx[HeadersContext]("headers")
        headersOp(block)

      /**
       * "config" level keyword for setting miscellaneous parameters
       * @param block nested definition of setting
       * @param context context provided by the "config" keyword
       */
      inline def options(block: OptionConfigScope)(using context: ConfigContext): Unit =
        catchRecursiveCtx[OptionConfigurationContext]("option")
        optionsOp(block)

      extension(x: String)
        /**
         * Simple utility keyword to create `(key, value)` headers (i.e. "Header-Name" to "Header-value")
         * @param value value of this header
         * @param context context provided by the the "headers" keyword
         */
        infix def to(value: String)(using context: HeadersContext): Unit = context.headers =
          context.headers + (x -> value)

    /**
     * Private collection of unsafe operators. The related safe versions are contained inside [[SafeOps]]
     */
    private[Config] object UnsafeOps:
      /**
       * Unsafe version of the one inside [[SafeOps]]
       * @param block nested definitions of configuration settings
       * @param globalScope global scooby scope (i.e. "scope: ...")
       * @tparam T type of the configuration (i.e. type of the result returned by this scraping behavior)
       */
      def configOp[T](block: GlobalConfigScope)(using globalScope: ConfigurationBuilder[T]): Unit =
        given context: ConfigContext = ConfigContext(ConfigOptions(), ClientConfiguration.default)
        block
        globalScope.configuration = globalScope.configuration
          .focus(_.crawlerConfiguration.networkOptions)     .replace(context.clientConfiguration)
          .focus(_.crawlerConfiguration.maxDepth)           .replace(context.options.maxDepth)
          .focus(_.coordinatorConfiguration.maxLinks)       .replace(context.options.maxLinks)

      /**
       * Unsafe version of the one inside [[SafeOps]]
       * @param block nested definitions of the headers value
       * @param context context provided by the "network" keyword
       */
      def headersOp(block: HeadersConfigScope)(using context: NetworkConfigurationContext): Unit =
          given builder: HeadersContext = HeadersContext(Map.empty)
          block
          context.config = context.config.focus(_.headers).replace(builder.headers)

      /**
       * Unsafe version of the one inside [[SafeOps]]
       * @param block   nested definitions of network settings
       * @param context context provided by the "config" keyword
       */
      def networkOp(block: NetworkConfigScope)(using context: ConfigContext): Unit =
          given builder: NetworkConfigurationContext = NetworkConfigurationContext(ClientConfiguration.default)
          block
          context.clientConfiguration = builder.config

      /**
       * Unsafe version of the one inside [[SafeOps]]
       * @param block   nested definition of setting
       * @param context context provided by the "config" keyword
       */
      def optionsOp(block: OptionConfigScope)(using context: ConfigContext): Unit =
          given builder: OptionConfigurationContext = OptionConfigurationContext(ConfigOptions())
          block
          context.options = builder.config

    /**
     * Utility type alias for monocle [[Lens]], used inside [[PropertyBuilder]] to manipulate (key,value) settings
     * of the Scooby configuration
     * @tparam T type of configuration under which this property is used (e.g. [[ClientConfiguration]])
     * @tparam V type of the value that the property is meant to have (e.g. [[Int]] or [[FiniteDuration]])
     */
    private type AccessProperty[T,V] = Lens[T, V]

    /**
     * Enum encapsulating all the `(key, value)` properties set during the configuration of Scooby.
     * @param property specifying how this property modifies the upper configuration
     * @tparam T type of configuration under which this property is used (e.g. [[ClientConfiguration]])
     * @tparam C type of the [[ConfigurationContext]] inside which this property <b>can</b> be set
     * @tparam V type of the value that the property is meant to have (e.g. [[Int]] or [[FiniteDuration]])
     */
    enum PropertyBuilder[T, C <: ConfigurationContext[T], V](property: AccessProperty[T,V]):

      /**
       * Property of [[ClientConfiguration]] for setting network timeout
       */
      case Timeout extends PropertyBuilder[ClientConfiguration, NetworkConfigurationContext, FiniteDuration] (
        GenLens[ClientConfiguration](_.networkTimeout)
      )
      /**
       * Property of [[ClientConfiguration]] for setting a maximum amount of requests
       */
      case MaxRequests extends PropertyBuilder[ClientConfiguration, NetworkConfigurationContext, Int] (
        GenLens[ClientConfiguration](_.maxRequests)
      )

      /**
       * Property of [[ConfigOptions]] for setting the maximum crawling depth
       */
      case MaxDepth extends PropertyBuilder[ConfigOptions, OptionConfigurationContext, Int] (
        GenLens[ConfigOptions](_.maxDepth)
      )
      /**
       * Property of [[ConfigOptions]] for setting the maximum amount of links that can be globally visited
       */
      case MaxLinks extends PropertyBuilder[ConfigOptions, OptionConfigurationContext, Int](
        GenLens[ConfigOptions](_.maxLinks)
      )

      /**
       * Utility keyword to assign a value to a property
       * @param propertyValue value of the property
       * @param context context where the target configuration can be found
       */
      infix def is(propertyValue: V)(using context: C): Unit =
        context.config = property.replace(propertyValue)(context.config)


  /**
   * Collection of the contexts used inside "config" section of the DSL
   */
  private[Config] object ConfigContexts:

    /**
     * Context representing the section under "config"
     * @param options miscellaneous options for Scooby
     * @param clientConfiguration network options
     */
    case class ConfigContext(var options: ConfigOptions,
                              var clientConfiguration: ClientConfiguration)

    /**
     * Context representing the section under "headers"
     * @param headers [[Map]] from headers' names to corresponding values
     */
    case class HeadersContext(var headers: Map[String, String])

    /**
     * Configuration container for miscellaneous options
     * @param maxDepth maximum crawling depth
     * @param maxLinks maximum amount of globally visited links
     */
    case class ConfigOptions(maxDepth: Int = 3, maxLinks: Int = 200)

    /**
     * Utility trait for contexts accessible via [[org.unibo.scooby.dsl.Config.ConfigOps.PropertyBuilder]]
     * @tparam T type of the configuration used by this context
     */
    trait ConfigurationContext[T]:
      var config: T

    /**
     * Context representing the section under "network"
     * @param config network configuration set inside this context
     */
    case class NetworkConfigurationContext(var config: ClientConfiguration)
      extends ConfigurationContext[ClientConfiguration]

    /**
     * Context representing the section under "options"
     * @param config configuration set inside this context
     */
    case class OptionConfigurationContext(var config: ConfigOptions)
      extends ConfigurationContext[ConfigOptions]

