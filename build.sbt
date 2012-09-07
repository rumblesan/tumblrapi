name := "tumblr scala api"

organization := "com.rumblesan"

version := "0.1"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2" % "1.12" % "test",
  "org.scribe" % "scribe" % "1.3.2",
  "com.codahale" %% "jerkson" % "0.5.0"
)

resolvers += "repo.codahale.com" at "http://repo.codahale.com"

initialCommands := "import com.rumblesan.tumblr.api._"

scalacOptions += "-deprecation"

