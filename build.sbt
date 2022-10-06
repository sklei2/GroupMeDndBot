import org.apache.tools.ant.taskdefs.optional.depend.Depend
import sbt._
import Dependencies._

scalaVersion := "2.13.8"

publish / skip := true

lazy val bot = project
  .enablePlugins(JavaAppPackaging)
  .settings(
    mainClass in Compile := Some("com.g5deathmarch.dndbot.Main")
  )
  .settings(topLevelDirectory := None)
  .settings(fork := true)
  .settings(
    organization := "com.g5deathmarch",
    name := "dnd-bot",
    scalaVersion := "2.13.8"
  )
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.http4s.server,
      Dependencies.http4s.client,
      Dependencies.http4s.dsl,
      Dependencies.http4s.circe,
      Dependencies.circeGeneric,
      Dependencies.pureConfig,
      Dependencies.logback,
      Dependencies.scalaLogging,
      Dependencies.scalaScraper
    )
  )
