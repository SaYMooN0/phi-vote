package backend.authservice.api.services
import backend.apishared.Configs
import backend.authservice.domain.services.{AccessTokenIssuer, IssuedAccessToken}
import backend.domainshared.AppUserId
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.RSAKeyProvider
import zio.*
import zio.config.magnolia.deriveConfig

import java.security.KeyFactory
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.time.Instant
import java.util.{Base64, Date}

final case class AccessTokenIssuingConfig(
                                           issuer: String,
                                           audience: String,
                                           keyId: String,
                                           publicKeyPem: String,
                                           privateKeyPem: String,
                                           ttlSeconds: Long
                                         )

object AccessTokenIssuingConfig {
  given Config[AccessTokenIssuingConfig] =    deriveConfig[AccessTokenIssuingConfig]
}



private final class AccessTokenIssuerLive private (
                                                    config: AccessTokenIssuingConfig,
                                                    algorithm: Algorithm
                                                  ) extends AccessTokenIssuer {

  override def issueFor(userId: AppUserId): Task[IssuedAccessToken] =
    for {
      now <- Clock.instant
      expiresAt = now.plusSeconds(config.ttlSeconds)

      token <- ZIO.attempt {
        JWT
          .create()
          .withKeyId(config.keyId)
          .withIssuer(config.issuer)
          .withAudience(config.audience)
          .withSubject(userId.value.toString)
          .withIssuedAt(Date.from(now))
          .withNotBefore(Date.from(now.minusSeconds(1)))
          .withExpiresAt(Date.from(expiresAt))
          .sign(algorithm)
      }
    } yield IssuedAccessToken(token, expiresAt)
}

private final class StaticRSAKeyProvider(
                                          keyId: String,
                                          publicKey: RSAPublicKey,
                                          privateKey: RSAPrivateKey
                                        ) extends RSAKeyProvider {

  override def getPublicKeyById(keyId: String): RSAPublicKey =
    publicKey

  override def getPrivateKey: RSAPrivateKey =
    privateKey

  override def getPrivateKeyId: String =
    keyId
}

private object RsaPemParser {

  def parsePublicKey(publicKeyPem: String): Task[RSAPublicKey] =
    ZIO.attempt {
      val normalized =
        publicKeyPem
          .replace("\\n", "\n")
          .replace("-----BEGIN PUBLIC KEY-----", "")
          .replace("-----END PUBLIC KEY-----", "")
          .replaceAll("\\s", "")

      val decoded = Base64.getDecoder.decode(normalized)
      val spec = X509EncodedKeySpec(decoded)

      KeyFactory
        .getInstance("RSA")
        .generatePublic(spec)
        .asInstanceOf[RSAPublicKey]
    }

  def parsePrivateKey(privateKeyPem: String): Task[RSAPrivateKey] =
    ZIO.attempt {
      val normalized =
        privateKeyPem
          .replace("\\n", "\n")
          .replace("-----BEGIN PRIVATE KEY-----", "")
          .replace("-----END PRIVATE KEY-----", "")
          .replaceAll("\\s", "")

      val decoded = Base64.getDecoder.decode(normalized)
      val spec = PKCS8EncodedKeySpec(decoded)

      KeyFactory
        .getInstance("RSA")
        .generatePrivate(spec)
        .asInstanceOf[RSAPrivateKey]
    }
}

object AccessTokenIssuerLive {

  import AccessTokenIssuingConfig.given

  val layer: ZLayer[
    AccessTokenIssuingConfig,
    Throwable,
    AccessTokenIssuer
  ] =
    ZLayer {
      for {
        config <- ZIO.service[AccessTokenIssuingConfig]

        publicKey <- RsaPemParser.parsePublicKey(config.publicKeyPem)
        privateKey <- RsaPemParser.parsePrivateKey(config.privateKeyPem)

        keyProvider = StaticRSAKeyProvider(
          keyId = config.keyId,
          publicKey = publicKey,
          privateKey = privateKey
        )

        algorithm = Algorithm.RSA256(keyProvider)
      } yield new AccessTokenIssuerLive(config, algorithm)
    }

  val configuredLayer: ZLayer[
    Any,
    Throwable,
    AccessTokenIssuer
  ] =
    Configs.makeLayer[AccessTokenIssuingConfig]("accessTokenIssuing") >>> layer
}