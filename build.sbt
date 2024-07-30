ThisBuild / version := sys.env.getOrElse("RELEASE_VERSION", "v0.1.0-SNAPSHOT")

ThisBuild / scalaVersion := "3.4.2"

ThisBuild / scalacOptions ++= Seq("-Wunused:all")

Global / onChangedBuildSource := ReloadOnSourceChanges

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}

lazy val idePackagePrefix = settingKey[String]("Package prefix for IDE").withRank(KeyRanks.Invisible)
lazy val root = (project in file("."))
  .settings(
    name := "PPS-22-Scooby",
    idePackagePrefix := "org.unibo.scooby"
  )

val AkkaVersion = "2.9.3"

resolvers += "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/"
resolvers += "Akka library repository".at("https://repo.akka.io/maven")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.5.6",
  "io.cucumber" %% "cucumber-scala" % "8.23.0" % Test,
  "io.cucumber" % "cucumber-junit" % "7.18.1" % Test,
  "com.github.sbt" % "junit-interface" % "0.13.3" % Test,
  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-http" % "10.6.3" % Test,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion % Test,
  "junit" % "junit" % "4.13.2" % Test,
  "com.typesafe.play" %% "play-json" % "2.10.5",
  "org.jsoup" % "jsoup" % "1.18.1",
  "com.softwaremill.sttp.client3" %% "core" % "3.9.7",
  "dev.optics" %% "monocle-core"  % "3.2.0",
  "dev.optics" %% "monocle-macro" % "3.2.0"
)

Test / parallelExecution := false