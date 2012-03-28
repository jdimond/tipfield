import sbt._
import Keys._
import com.github.siasia._
import PluginKeys._
import WebPlugin._
import WebappPlugin._

object LiftProjectBuild extends Build {
  override lazy val settings = super.settings ++ buildSettings

  lazy val buildSettings = Seq(
    organization := "de.dimond",
    version      := "0.1-SNAPSHOT",
    scalaVersion := "2.9.1")

  def customWebSettings = webSettings ++ Seq(
    scanDirectories in Compile := Nil
  )

  lazy val tippspiel = Project(
    id = "tippspiel",
    base = file("."),
    settings = defaultSettings ++ customWebSettings)

  lazy val defaultSettings = Defaults.defaultSettings ++ Seq(
    name := "tippspiel",
    resolvers ++= Seq(
      "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases",
      "Java.net Maven2 Repository" at "http://download.java.net/maven/2/"),

    libraryDependencies ++= {
      val liftVersion = "2.4"
      Seq(
        "net.liftweb" %% "lift-webkit" % liftVersion % "compile",
        "net.liftweb" %% "lift-mapper" % liftVersion % "compile",
        "net.liftweb" %% "lift-json" % liftVersion % "compile",
        "org.mortbay.jetty" % "jetty" % "6.1.22" % "container",
        "ch.qos.logback" % "logback-classic" % "1.0.0" % "compile",
        "org.scalatest" %% "scalatest" % "1.6.1" % "test",
        "junit" % "junit" % "4.10" % "test",
        "org.scala-tools.time" % "time_2.9.1" % "0.5",
        "joda-time" % "joda-time" % "2.0",
        "org.joda" % "joda-convert" % "1.1",
        "postgresql" % "postgresql" % "8.4-702.jdbc4"
      )
    },

    // compile options
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked"),
    javacOptions  ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),

    // show full stack traces
    testOptions in Test += Tests.Argument("-oF")
  )
}

