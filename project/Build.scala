import sbt._
import sbt.Keys._

object UtilsBuild extends Build {

  lazy val buildSettings = Defaults.defaultSettings ++ Seq(

    organization := "com.rumblesan.util",

    scalaVersion := "2.10.1",

    version := "0.2.0",

    crossScalaVersions := Seq("2.9.2", "2.9.3", "2.10.1")

  )

  // Dependencies.

  // This helps make sure we get the correct specs2 version
  // there are different versions for scala 2.9 and 2.10
  def specs2Dependencies(scalaVersion: String) = {
    val Old = """2\.9\..*""".r
    scalaVersion match {
      case Old() => Seq("org.specs2" %% "specs2" % "1.12.4" % "test")
      case _ => Seq("org.specs2" %% "specs2" % "1.14" % "test")
    }
  }

  lazy val scribe = "org.scribe" % "scribe" % "1.3.2"

  lazy val appDependencies = Seq(
    libraryDependencies <++= scalaVersion(specs2Dependencies(_)),
    libraryDependencies += scribe
  )

  lazy val utils = Project(

    id = "tumblrapi",
    base = file("."),

    settings = Defaults.defaultSettings ++ buildSettings ++ appDependencies

  ).settings(
    scalacOptions ++= Seq("-deprecation", "-feature", "-language:_")
  )

}
