package org.unibo.scooby

import io.cucumber.junit.{Cucumber, CucumberOptions}
import org.junit.runner.RunWith

@RunWith(classOf[Cucumber])
@CucumberOptions(
  features = Array("classpath:features"),
  plugin = Array("pretty", "html:target/cucumber-reports.html")
)
class TestRunner
