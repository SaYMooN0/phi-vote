package backend.authservice.api.services

import backend.apishared.Configs
import backend.authservice.domain.services.{PasswordHashingConfig, PasswordHashingService, UserPassword}
import backend.authservice.domain.shared.PasswordHash
import de.mkammerer.argon2.Argon2Factory.Argon2Types
import de.mkammerer.argon2.{Argon2, Argon2Factory}
import zio.*
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.TypesafeConfigProvider

private final class PasswordHashingServiceLive private(
                                                        config: PasswordHashingConfig,
                                                        argon2: Argon2
                                                      ) extends PasswordHashingService {

  override def hash(password: UserPassword): Task[PasswordHash] =
    ZIO.attemptBlocking {
      val passwordChars = password.value.toCharArray

      try {
        PasswordHash.unsafeFrom(
          argon2.hash(
            config.iterations,
            config.memoryKiB,
            config.parallelism,
            passwordChars
          )
        )
      } finally {
        argon2.wipeArray(passwordChars)
      }
    }

  override def verify(passwordToVerify: UserPassword, hash: PasswordHash): Task[Boolean] = {
    ZIO.attemptBlocking {
      val passwordChars = passwordToVerify.value.toCharArray

      try {
        argon2.verify(
          hash.value,
          passwordChars
        )
      } finally {
        argon2.wipeArray(passwordChars)
      }
    }
  }
}

object PasswordHashingConfig {
  given Config[PasswordHashingConfig] = deriveConfig[PasswordHashingConfig]
}

object PasswordHashingServiceLive {

  import PasswordHashingConfig.given

  val layer: ZLayer[
    PasswordHashingConfig,
    Throwable,
    PasswordHashingService
  ] =
    ZLayer {
      for {
        config <- ZIO.service[PasswordHashingConfig]
        argon2 <- ZIO.attempt {
          Argon2Factory.create(Argon2Types.ARGON2i, config.saltLength, config.hashLength)
        }
      } yield new PasswordHashingServiceLive(config, argon2)
    }

  val configuredLayer: ZLayer[
    Any,
    Throwable,
    PasswordHashingService
  ] =
    Configs.makeLayer[PasswordHashingConfig]("passwordHashing") >>> layer
}

object PasswordHashingServiceDemo extends ZIOAppDefault {
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.setConfigProvider(TypesafeConfigProvider.fromResourcePath())

  private val program = for {
    service <- ZIO.service[PasswordHashingService]
    password = UserPassword.createFrom("123").toOption.get
    hash <- service.hash(password)
    _ <- Console.printLine(hash.value)
    verified <- service.verify(password, hash)
    _ <- Console.printLine(s"verified = $verified")
  } yield ()

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    program.provide(
      PasswordHashingServiceLive.configuredLayer
    )
}