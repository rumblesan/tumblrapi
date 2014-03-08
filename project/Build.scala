import sbt._
import sbt.Keys._

object UtilsBuild extends Build {

  lazy val buildSettings = Defaults.defaultSettings ++ Seq(

    organization := "com.rumblesan.util",

    scalaVersion := "2.10.3",

    version := "0.2.0"

  )


  lazy val specs2 = "org.specs2" %% "specs2" % "2.3.10" % "test"
  lazy val scribe = "org.scribe" % "scribe" % "1.3.2"

  lazy val appDependencies = Seq(
    libraryDependencies += specs2,
    libraryDependencies += scribe
  )


  lazy val utils = Project(

    id = "tumblrapi",

    base = file("."),

    settings = Defaults.defaultSettings ++ buildSettings ++ appDependencies

  ).settings(

    scalacOptions ++= Seq("-feature", "-language:_", "-deprecation")

  )

}
