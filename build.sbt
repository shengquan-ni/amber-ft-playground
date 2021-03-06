name := "akka-quickstart-scala"

version := "1.0"

scalaVersion := "2.13.1"

lazy val akkaVersion = "2.6.11"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.twitter" %% "util-core" % "20.9.0",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.1.0" % Test
)
