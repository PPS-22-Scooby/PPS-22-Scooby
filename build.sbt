ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.3"

lazy val root = (project in file("."))
  .settings(
    name := "PPS-22-Scooby",
    idePackagePrefix := Some("org.unibo.scooby")
  )

resolvers += "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/"

libraryDependencies ++= Seq(
  "io.cucumber" %% "cucumber-scala" % "8.23.0" % Test,
  "io.cucumber" % "cucumber-junit" % "7.18.0" % Test,
  "junit" % "junit" % "4.13.2" % Test,
)