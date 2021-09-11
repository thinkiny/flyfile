ThisBuild / organization := "com.thinkiny"
ThisBuild / scalaVersion := "2.13.6"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val commonSettings = Seq(
  //graalVMNativeImageOptions ++= Seq("--no-fallback"),
  libraryDependencies ++= Seq(
    "com.twitter" %% "finatra-http" % "21.2.0",
    "ch.qos.logback" % "logback-classic" % "1.2.5",
    "net.lingala.zip4j" % "zip4j" % "2.9.0"
  ),
  scalacOptions ++= Seq("-feature", "-deprecation", "-Ywarn-unused"),
  fork := true
)

lazy val server = (project in file("server"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    commonSettings,
    name := "flyfile-server"
  )

lazy val client = (project in file("client"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    commonSettings,
    name := "flyfile-client",
    libraryDependencies ++= Seq("com.lihaoyi" %% "mainargs" % "0.2.1")
  )
