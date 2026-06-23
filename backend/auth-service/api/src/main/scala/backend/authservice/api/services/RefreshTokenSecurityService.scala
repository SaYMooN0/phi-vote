package backend.authservice.api.services

import backend.authservice.domain.entities.{RefreshTokenHash, RefreshTokenPlain}
import backend.authservice.domain.services.{RefreshTokenConfig, RefreshTokenSecurityService}
import zio.config.magnolia.deriveConfig
import zio.{Config, Task, ZIO, ZLayer}

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


private final class RefreshTokenSecurityServiceLive private(
                                                             config: RefreshTokenConfig,
                                                             secureRandom: SecureRandom
                                                           ) extends RefreshTokenSecurityService {

  override def generate: Task[RefreshTokenPlain] = ZIO.attempt {
    val bytes = Array.ofDim[Byte](32)
    secureRandom.nextBytes(bytes)

    val value = Base64
      .getUrlEncoder
      .withoutPadding()
      .encodeToString(bytes)

    RefreshTokenPlain.unsafeFrom(value)
  }

  override def hash(token: RefreshTokenPlain): Task[RefreshTokenHash] =
    ZIO.attempt {
      val mac = Mac.getInstance("HmacSHA256")
      val secretKey = SecretKeySpec(
        config.hmacSecret.getBytes("UTF-8"),
        "HmacSHA256"
      )

      mac.init(secretKey)

      val hash = Base64
        .getUrlEncoder
        .withoutPadding()
        .encodeToString(
          mac.doFinal(token.value.getBytes("UTF-8"))
        )

      RefreshTokenHash.unsafeFrom(hash)
    }
}

object RefreshTokenConfig {
  given Config[RefreshTokenConfig] = deriveConfig[RefreshTokenConfig]
}

object RefreshTokenSecurityServiceLive {

  val layer: ZLayer[
    RefreshTokenConfig,
    Nothing,
    RefreshTokenSecurityService
  ] =
    ZLayer {
      for {
        config <- ZIO.service[RefreshTokenConfig]
        secureRandom <- ZIO.succeed(SecureRandom())
      } yield new RefreshTokenSecurityServiceLive(config, secureRandom)
    }
}