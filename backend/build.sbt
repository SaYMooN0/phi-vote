ThisBuild / scalaVersion := "3.3.8"
ThisBuild / organization := "com.example"
ThisBuild / version := "0.1.0"

lazy val javaVersion = "21"

lazy val zioVersion = "2.1.26"
lazy val zioHttpVersion = "3.8.0"
lazy val zioJsonVersion = "0.7.44"
lazy val quillVersion = "4.8.6"
lazy val postgresVersion = "42.7.4"

ThisBuild / dependencyOverrides ++= Seq(
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion,
  "dev.zio" %% "zio-json" % zioJsonVersion
)

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
  .aggregate(apiShared, dbShared, authService, votingService)
  .settings(
    name := "backend",
    publish / skip := true
  )

lazy val dbShared = (project in file("./lib/db-shared"))
  .settings(commonSettings)
  .settings(
    name := "db-shared",

    libraryDependencies ++= Seq(
      "io.getquill" %% "quill-jdbc-zio" % quillVersion,
      "org.postgresql" % "postgresql" % postgresVersion
    ),
  )

lazy val apiShared = (project in file("./lib/api-shared"))
  .settings(commonSettings)
  .settings(
    name := "api-shared",

    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-http" % zioHttpVersion,
      "dev.zio" %% "zio-json" % zioJsonVersion
    )
  )

lazy val authService = (project in file("auth-service"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .dependsOn(apiShared)
  .dependsOn(dbShared)
  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(
    name := "auth-service",

    libraryDependencies ++= Seq(
      "com.github.f4b6a3" % "uuid-creator" % "6.1.1"
    ),

    Compile / mainClass := Some("backend.auth.AuthServiceMain"),

    Docker / packageName := "backend/auth-service",
    dockerExposedPorts := Seq(8180)
  )

lazy val votingService = (project in file("voting-service"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .dependsOn(apiShared)
  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(
    name := "voting-service",

    Compile / mainClass := Some("backend.voting.VotingServiceMain"),

    Docker / packageName := "backend/voting-service",
    dockerExposedPorts := Seq(8181)
  )