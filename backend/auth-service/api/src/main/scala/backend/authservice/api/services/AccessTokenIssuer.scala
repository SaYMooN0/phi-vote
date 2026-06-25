package backend.authservice.api.services

import backend.apishared.Configs
import backend.apishared.services.RsaPemParser
import backend.authservice.api.configs.AccessTokenIssuingConfig
import backend.authservice.domain.services.{AccessTokenIssuer, IssuedAccessToken}
import backend.domainshared.AppUserId
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.RSAKeyProvider
import zio.*
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.util.Date


private final class AccessTokenIssuerLive private(
  config: AccessTokenIssuingConfig,
  algorithm: Algorithm
) extends AccessTokenIssuer {

  override def issueFor(userId: AppUserId): Task[IssuedAccessToken] = {
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
}

private final class StaticRSAKeyProvider(
  keyId: String,
  publicKey: RSAPublicKey,
  privateKey: RSAPrivateKey
) extends RSAKeyProvider {

  override def getPublicKeyById(keyId: String): RSAPublicKey = publicKey

  override def getPrivateKey: RSAPrivateKey = privateKey

  override def getPrivateKeyId: String = keyId
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
        keyProvider = StaticRSAKeyProvider(config.keyId, publicKey, privateKey)
        algorithm = Algorithm.RSA256(keyProvider)
      } yield new AccessTokenIssuerLive(config, algorithm)
    }

  val configuredLayer: ZLayer[
    Any,
    Throwable,
    AccessTokenIssuer
  ] =
    Configs.makeLayer[AccessTokenIssuingConfig]("webAuth.jwtToken") >>> layer
}