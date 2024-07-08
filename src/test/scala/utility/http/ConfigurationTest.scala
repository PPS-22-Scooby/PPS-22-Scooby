package org.unibo.scooby
package utility.http


import utility.http.Configuration.Property
import utility.http.Configuration.Property.{MaxRequests, NetworkTimeout}

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class ConfigurationTest extends AnyFlatSpec with should.Matchers:

  "A default Configuration" should "return a default NetworkTimeout" in:
    val config = Configuration.default
    config.property[NetworkTimeout, FiniteDuration] should be(Some(5.seconds))
    config.property[MaxRequests, Int] should be(None)