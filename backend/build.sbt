import sbt.util.Logger

import scala.io.Source

ThisBuild / scalaVersion := "3.8.3"
ThisBuild / organization := "com.example"
ThisBuild / version := "0.1.0"

lazy val javaVersion = "21"

lazy val zioVersion = "2.1.26"
lazy val zioHttpVersion = "3.8.0"
lazy val zioJsonVersion = "0.7.44"
lazy val zioConfigVersion = "4.0.7"

lazy val quillVersion = "4.8.6"
lazy val postgresVersion = "42.7.4"

lazy val argon2Version = "2.12"
lazy val javaMailVersion = "1.6.2"

ThisBuild / dependencyOverrides ++= Seq(
  "dev.zio" %% "zio" % zioVersion,

  "dev.zio" %% "zio-streams" % zioVersion,
  "dev.zio" %% "zio-json" % zioJsonVersion
)

lazy val root = (project in file("."))
  .aggregate(
    domainShared,
    dbShared,
    apiShared,
    //auth service
    authServiceDomain,
    authServiceDb,
    authServiceApi,
    //voting service
    votingService
  )
  .settings(
    name := "backend",
    publish / skip := true
  )

lazy val domainShared = (project in file("./lib/domain-shared"))
  .settings(commonSettings)
  .settings(
    name := "domain-shared",
    libraryDependencies ++= Seq(
      "com.github.f4b6a3" % "uuid-creator" % "6.1.1",
      "dev.zio" %% "zio" % zioVersion
    )
  )

lazy val dbShared = (project in file("./lib/db-shared"))
  .dependsOn(domainShared)
  .settings(commonSettings)
  .settings(
    name := "db-shared",

    libraryDependencies ++= Seq(
      "io.getquill" %% "quill-jdbc-zio" % quillVersion,
      "org.postgresql" % "postgresql" % postgresVersion
    )
  )

lazy val apiShared = (project in file("./lib/api-shared"))
  .settings(commonSettings)
  .settings(
    name := "api-shared",

    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-http" % zioHttpVersion,
      "dev.zio" %% "zio-json" % zioJsonVersion,
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion
    )
  )


lazy val authServiceDomain = (project in file("./auth-service/domain"))
  .settings(commonSettings)
  .dependsOn(domainShared)
  .settings(
    name := "auth-service-domain"
  )
lazy val authServiceDb = (project in file("./auth-service/db"))
  .settings(commonSettings)
  .dependsOn(dbShared, authServiceDomain)
  .settings(
    name := "auth-service-db"
  )
lazy val authServiceApi = (project in file("./auth-service/api"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .dependsOn(apiShared, authServiceDb)
  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(dotenvSettings)
  .settings(
    name := "auth-service-api",

    libraryDependencies ++= Seq(
      "de.mkammerer" % "argon2-jvm" % argon2Version,
      "com.sun.mail" % "javax.mail" % javaMailVersion,

    ),

    Compile / mainClass := Some("AuthServiceMain"),
    Docker / packageName := "backend/auth-service",
    dockerExposedPorts := Seq(8180)
  )
lazy val votingService = (project in file("voting-service"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .dependsOn(
    domainShared,
    apiShared
  )
  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(dotenvSettings)
  .settings(
    name := "voting-service",

    Compile / mainClass := Some("backend.votingservice.VotingServiceMain"),

    Docker / packageName := "backend/voting-service",
    dockerExposedPorts := Seq(8181)
  )

lazy val commonSettings = Seq(
  javacOptions ++= Seq(
    "--release",
    javaVersion
  ),

  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked"
  )
)

lazy val dockerSettings = Seq(
  dockerBaseImage := s"eclipse-temurin:$javaVersion-jre"
)

lazy val dotenvSettings = Seq(
  Compile / run / fork := true,
  Compile / run / envVars ++= {
    val log = sLog.value
    val rootDir = (ThisBuild / baseDirectory).value
    loadDotEnv(rootDir / ".env", log)
  },
  Test / fork := true,
  Test / envVars ++= {
    val log = sLog.value
    val rootDir = (ThisBuild / baseDirectory).value
    loadDotEnv(rootDir / ".env", log)
  }
)

def loadDotEnv(file: File, log: Logger): Map[String, String] = {
  if (!file.exists()) {
    log.info(s"[dotenv] ${file.getAbsolutePath} not found, skipping")
    Map.empty
  } else {
    val source = Source.fromFile(file, "UTF-8")

    try {
      val parsed = source
        .getLines()
        .zipWithIndex
        .flatMap { case (rawLine, index) =>
          val line = rawLine.trim

          if (line.isEmpty || line.startsWith("#")) {
            None
          } else {
            line.split("=", 2).toList match {
              case key :: value :: Nil if key.trim.nonEmpty => Some(key.trim -> value.trim)
              case _ => {
                log.warn(s"[dotenv] ${file.getName}:${index + 1}: cannot parse line, skipping")
                None
              }
            }
          }
        }
        .toVector

      val duplicates =
        parsed
          .groupBy(_._1)
          .collect { case (key, values) if values.size > 1 => key }
          .toVector
          .sorted

      if (duplicates.nonEmpty) {
        log.warn(s"[dotenv] duplicate keys found, last value wins: ${duplicates.mkString(", ")}")
      }

      val env = parsed.toMap

      log.info(s"[dotenv] loaded ${env.size} variables from ${file.getAbsolutePath}")
      log.info(s"[dotenv] keys: ${env.keys.toVector.sorted.mkString(", ")}")

      env
    } finally {
      source.close()
    }
  }
}
