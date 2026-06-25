package backend.apishared.services

import backend.apishared.*
import backend.domainshared.AppUserId
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.*
import com.auth0.jwt.interfaces.{DecodedJWT, RSAKeyProvider}
import zio.*
import zio.config.magnolia.deriveConfig

import java.security.KeyFactory
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.time.Instant
import java.util.{Base64, UUID}


trait AccessTokenReader {
  def read(token: String): IO[AccessTokenReadErr.type, AccessTokenClaims]
}


final case class AccessTokenClaims(userId: AppUserId, tokenId: String, expiresAt: Instant)

object AccessTokenReadErr;

private final class PublicOnlyRSAKeyProvider(publicKey: RSAPublicKey) extends RSAKeyProvider {
  override def getPublicKeyById(keyId: String): RSAPublicKey = publicKey

  override def getPrivateKey: RSAPrivateKey = null

  override def getPrivateKeyId: String = null
}

object RsaPemParser {
  def parsePublicKey(publicKeyPem: String): Task[RSAPublicKey] = {
    ZIO.attempt {
      val normalized = publicKeyPem
        .replace("\\n", "\n")
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replaceAll("\\s", "")

      val decoded = Base64.getDecoder.decode(normalized)
      val spec    = X509EncodedKeySpec(decoded)

      KeyFactory
        .getInstance("RSA")
        .generatePublic(spec)
        .asInstanceOf[RSAPublicKey]
    }
  }

  def parsePrivateKey(privateKeyPem: String): Task[RSAPrivateKey] = {
    ZIO.attempt {
      val normalized =
        privateKeyPem
          .replace("\\n", "\n")
          .replace("-----BEGIN PRIVATE KEY-----", "")
          .replace("-----END PRIVATE KEY-----", "")
          .replaceAll("\\s", "")

      val decoded = Base64.getDecoder.decode(normalized)
      val spec    = PKCS8EncodedKeySpec(decoded)

      KeyFactory
        .getInstance("RSA")
        .generatePrivate(spec)
        .asInstanceOf[RSAPrivateKey]
    }
  }
}

final case class AccessTokenReaderConfig(
  issuer: String,
  audience: String,
  publicKeyPem: String,
  clockSkewSeconds: Long
)

object AccessTokenReaderConfig {
  given Config[AccessTokenReaderConfig] = deriveConfig[AccessTokenReaderConfig]
}


private final class AccessTokenReaderLive private(
  config: AccessTokenReaderConfig,
  algorithm: Algorithm
) extends AccessTokenReader {

  private val verifier = JWT
    .require(algorithm)
    .withIssuer(config.issuer)
    .withAudience(config.audience)
    .acceptLeeway(config.clockSkewSeconds)
    .build()

  override def read(token: String): IO[AccessTokenReadErr.type, AccessTokenClaims] = {
    for {
      decoded <- ZIO
        .attempt(verifier.verify(token))
        .mapError(_ => AccessTokenReadErr)

      userId <- parseUserId(decoded.getSubject)
      tokenId <- parseTokenId(decoded)
      expiresAt <- parseExpiresAt(decoded)
    } yield AccessTokenClaims(userId, tokenId, expiresAt)
  }


  private def parseUserId(subject: String): IO[AccessTokenReadErr.type, AppUserId] = {
    ZIO
      .attempt(AppUserId(UUID.fromString(subject)))
      .mapError(_ => AccessTokenReadErr)
  }

  private def parseTokenId(decoded: DecodedJWT): IO[AccessTokenReadErr.type, String] = {
    Option(decoded.getId) match {
      case Some(value) if value.nonEmpty => ZIO.succeed(value)
      case _ => ZIO.fail(AccessTokenReadErr)
    }
  }

  private def parseExpiresAt(decoded: DecodedJWT): IO[AccessTokenReadErr.type, Instant] = {
    Option(decoded.getExpiresAt) match {
      case Some(value) => ZIO.succeed(value.toInstant)
      case None => ZIO.fail(AccessTokenReadErr)
    }
  }
}


object AccessTokenReaderLive {

  import AccessTokenReaderConfig.given

  val layer: ZLayer[AccessTokenReaderConfig, Throwable, AccessTokenReader] =
    ZLayer {
      for {
        config <- ZIO.service[AccessTokenReaderConfig]
        publicKey <- RsaPemParser.parsePublicKey(config.publicKeyPem)
        algorithm = Algorithm.RSA256(PublicOnlyRSAKeyProvider(publicKey))
      } yield AccessTokenReaderLive(config, algorithm)
    }

  val configuredLayer: ZLayer[Any, Throwable, AccessTokenReader] =
    Configs.makeLayer[AccessTokenReaderConfig]("accessTokenReader") >>> layer
}