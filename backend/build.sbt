import sbt.util.Logger

import scala.io.Source

ThisBuild / scalaVersion := "3.8.3"
ThisBuild / organization := "com.example"
ThisBuild / version := "0.1.0"

lazy val javaVersion = "21"

lazy val zioVersion       = "2.1.26"
lazy val zioHttpVersion   = "3.8.0"
lazy val zioJsonVersion   = "0.7.44"
lazy val zioConfigVersion = "4.0.7"

lazy val quillVersion    = "4.8.6"
lazy val postgresVersion = "42.7.4"

lazy val argon2Version   = "2.12"
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
  .dependsOn(domainShared)
  .settings(commonSettings)
  .settings(
    name := "api-shared",

    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-http" % zioHttpVersion,
      "dev.zio" %% "zio-json" % zioJsonVersion,
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
      "com.auth0" % "java-jwt" % "4.5.2"
    )
  )


lazy val authServiceDomain = (project in file("./auth-service/domain"))
  .settings(commonSettings)
  .dependsOn(domainShared)
  .settings(
    name := "auth-service-domain"
  )
lazy val authServiceDb     = (project in file("./auth-service/db"))
  .settings(commonSettings)
  .dependsOn(dbShared, authServiceDomain)
  .settings(
    name := "auth-service-db"
  )
lazy val authServiceApi    = (project in file("./auth-service/api"))
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

    Compile / mainClass := Some("backend.authservice.api.AuthServiceMain"),
    Docker / packageName := "backend/auth-service",
    dockerExposedPorts := Seq(8180)
  )
lazy val votingService     = (project in file("voting-service"))
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
    val log     = sLog.value
    val rootDir = (ThisBuild / baseDirectory).value
    loadDotEnv(rootDir / ".env", log)
  },
  Test / fork := true,
  Test / envVars ++= {
    val log     = sLog.value
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
      val lines  = source.getLines().toVector
      val parsed = parseDotEnvLines(lines, file.getName, log)

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

def parseDotEnvLines(lines: Vector[String], fileName: String, log: Logger): Vector[(String, String)] = {
  val parsed = Vector.newBuilder[(String, String)]
  var index  = 0

  while (index < lines.length) {
    val rawLine     = lines(index)
    val trimmedLine = rawLine.trim

    if (trimmedLine.isEmpty || trimmedLine.startsWith("#")) {
      index += 1
    } else {
      val equalsIndex = rawLine.indexOf("=")

      if (equalsIndex < 0) {
        log.warn(s"[dotenv] $fileName:${index + 1}: cannot parse line, skipping")
        index += 1
      } else {
        val key      = rawLine.substring(0, equalsIndex).trim
        val rawValue = rawLine.substring(equalsIndex + 1).trim

        if (key.isEmpty) {
          log.warn(s"[dotenv] $fileName:${index + 1}: empty key, skipping")
          index += 1
        } else if (rawValue.startsWith("'")) {
          val parsedMultiline = parseSingleQuotedValue(
            key = key,
            firstValuePart = rawValue.drop(1),
            firstLineIndex = index,
            lines = lines,
            fileName = fileName,
            log = log
          )

          parsedMultiline match {
            case Some((value, nextIndex)) => {
              parsed += key -> value
              index = nextIndex
            }
            case None => index = lines.length
          }
        } else {
          parsed += key -> rawValue
          index += 1
        }
      }
    }
  }

  parsed.result()
}

def parseSingleQuotedValue(
  key: String,
  firstValuePart: String,
  firstLineIndex: Int,
  lines: Vector[String],
  fileName: String,
  log: Logger
): Option[(String, Int)] = {
  val firstClosingQuoteIndex = findClosingSingleQuote(firstValuePart)

  if (firstClosingQuoteIndex >= 0) {
    val value = firstValuePart.take(firstClosingQuoteIndex)
    val rest  = firstValuePart.drop(firstClosingQuoteIndex + 1).trim

    if (rest.nonEmpty && !rest.startsWith("#")) {
      log.warn(
        s"[dotenv] $fileName:${firstLineIndex + 1}: extra characters after closing quote for key '$key', ignoring"
      )
    }

    Some(value -> (firstLineIndex + 1))
  } else {
    val valueBuilder = new StringBuilder(firstValuePart)

    var index  = firstLineIndex + 1
    var closed = false

    while (index < lines.length && !closed) {
      val line              = lines(index)
      val closingQuoteIndex = findClosingSingleQuote(line)

      valueBuilder.append('\n')

      if (closingQuoteIndex >= 0) {
        valueBuilder.append(line.take(closingQuoteIndex))

        val rest = line.drop(closingQuoteIndex + 1).trim

        if (rest.nonEmpty && !rest.startsWith("#")) {
          log.warn(
            s"[dotenv] $fileName:${index + 1}: extra characters after closing quote for key '$key', ignoring"
          )
        }

        closed = true
      } else {
        valueBuilder.append(line)
      }

      index += 1
    }

    if (closed) {
      Some(valueBuilder.result() -> index)
    } else {
      log.warn(
        s"[dotenv] $fileName:${firstLineIndex + 1}: unclosed single-quoted value for key '$key', skipping"
      )
      None
    }
  }
}

def findClosingSingleQuote(value: String): Int = {
  var index   = 0
  var escaped = false

  while (index < value.length) {
    val char = value.charAt(index)

    if (escaped) {
      escaped = false
    } else if (char == '\\') {
      escaped = true
    } else if (char == '\'') {
      return index
    }

    index += 1
  }

  -1
}

