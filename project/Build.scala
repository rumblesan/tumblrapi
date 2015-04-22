import sbt._
import sbt.Keys._

object UtilsBuild extends Build {

  lazy val buildSettings = Defaults.defaultSettings ++ Seq(

    organization := "com.rumblesan.util",

    scalaVersion := "2.11.6",

    version := "0.3.0"

  )


  lazy val scribe = "org.scribe" % "scribe" % "1.3.2"
  lazy val config = "com.typesafe" % "config" % "1.0.1"

  lazy val appDependencies = Seq(
    libraryDependencies += scribe,
    libraryDependencies += config
  )


  lazy val utils = Project(

    id = "tumblrapi",

    base = file("."),

    settings = Defaults.defaultSettings ++ buildSettings ++ appDependencies

  ).settings(

    scalacOptions ++= Seq("-feature", "-language:_", "-deprecation")

  )

}
