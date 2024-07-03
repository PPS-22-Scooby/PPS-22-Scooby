import scala.collection.immutable.Seq

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.4.2"

ThisBuild / semanticdbEnabled := true

ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

ThisBuild / scalacOptions ++= Seq("-Wunused:all")

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
  .settings(
    name := "PPS-22-Scooby",
    idePackagePrefix := Some("org.unibo.scooby")
  )

val AkkaVersion = "2.9.3"

resolvers += "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/"
resolvers += "Akka library repository".at("https://repo.akka.io/maven")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.5.6",
  "org.typelevel" %% "cats-core" % "2.12.0",
  "io.cucumber" %% "cucumber-scala" % "8.23.0" % Test,
  "io.cucumber" % "cucumber-junit" % "7.18.0" % Test,
  "com.github.sbt" % "junit-interface" % "0.13.3" % Test,
  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-http" % "10.6.3" % Test,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion % Test,
  "junit" % "junit" % "4.13.2" % Test,
  "com.typesafe.play" %% "play-json" % "2.10.5" % Test,
  "org.jsoup" % "jsoup" % "1.17.2",
  "com.softwaremill.sttp.client3" %% "core" % "3.9.7"
)

scalafixOnCompile := true
Test / parallelExecution := false