import com.typesafe.sbt.packager.docker.*

ThisBuild / scalaVersion := "3.3.8"
ThisBuild / organization := "com.example"
ThisBuild / version := "0.1.0"

lazy val javaVersion = "21"
lazy val zioVersion = "2.1.26"
lazy val zioHttpVersion = "3.11.2"

lazy val commonSettings = Seq(
  javacOptions ++= Seq(
    "--release",
    javaVersion
  ),

  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked",
    "-java-output-version",
    javaVersion
  )
)

lazy val dockerSettings = Seq(
  dockerBaseImage := s"eclipse-temurin:$javaVersion-jre"
)

lazy val root = (project in file("."))
  .aggregate(shared, authService, votingService)
  .settings(
    name := "backend",
    publish / skip := true
  )

lazy val shared = (project in file("shared"))
  .settings(commonSettings)
  .settings(
    name := "shared",

    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion
    )
  )

lazy val authService = (project in file("auth-service"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .dependsOn(shared)
  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(
    name := "auth-service",
    Compile / mainClass := Some("backend.auth.AuthServiceMain"),

    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-http" % zioHttpVersion
    ),

    Docker / packageName := "backend/auth-service",
    dockerExposedPorts := Seq(8080)
  )

lazy val votingService = (project in file("voting-service"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .dependsOn(shared)
  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(
    name := "voting-service",
    Compile / mainClass := Some("backend.voting.VotingServiceMain"),

    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-http" % zioHttpVersion
    ),

    Docker / packageName := "backend/voting-service",
    dockerExposedPorts := Seq(8080)
  )