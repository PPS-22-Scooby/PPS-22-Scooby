package org.unibo.scooby
package utility.http


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.unibo.scooby.utility.http.Configuration.Property

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import org.unibo.scooby.utility.http.Configuration.Property.{MaxRequests, NetworkTimeout}

class ConfigurationTest extends AnyFlatSpec with should.Matchers:

  "A default Configuration" should "return a default NetworkTimeout" in:
    val config = Configuration.default
    config.property[NetworkTimeout, FiniteDuration] should be(Some(5.seconds))
    config.property[MaxRequests, Int] should be(None)