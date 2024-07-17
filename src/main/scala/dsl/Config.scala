package org.unibo.scooby
package dsl
import utility.http.Configuration.ClientConfiguration

object Config:

  case class ConfigContext(
                            clientConfiguration: ClientConfiguration
                          )

  case class ConfigOptions(
                            maxDepth: Int = 3,
                            maxLinks: Int = 200
                          )


  def config(using ConfigurationBuilder[T]): Unit = ???

  object NetworkConfiguration
  object CrawlerGlobalConfiguration

