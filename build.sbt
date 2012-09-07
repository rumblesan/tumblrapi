name := "tumblr_scala_api"

organization := "com.rumblesan"

version := "0.1"

scalaVersion := "2.9.2"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2" % "1.12" % "test"
)

initialCommands := "import com.rumblesan.tumblr_scala_api._"
