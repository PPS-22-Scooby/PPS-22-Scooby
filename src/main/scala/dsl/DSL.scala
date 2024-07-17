package org.unibo.scooby
package dsl
import core.scooby.Configuration

object DSL:

  export Config.*
  export Crawl.*
  export Scrape.*
  export Export.*


  class ConfigurationBuilder[T](var configuration: Configuration[T]):
    
    def build: Configuration[T] = configuration

    