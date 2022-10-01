import sbt._

object Dependencies {

  val circeGeneric = "io.circe" %% "circe-generic" % "0.14.3"
  val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.17.1"
  val logback = "ch.qos.logback" % "logback-classic" % "1.2.11"
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"

  object http4s {
    private val version = "0.23.16"

    val dsl = "org.http4s" %% "http4s-dsl" % version
    val server = "org.http4s" %% "http4s-ember-server" % version
    val client = "org.http4s" %% "http4s-ember-client" % version
    val circe = "org.http4s" %% "http4s-circe" % version
  }

}
