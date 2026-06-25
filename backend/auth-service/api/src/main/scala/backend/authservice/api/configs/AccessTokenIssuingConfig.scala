package backend.authservice.api.configs

import zio.Config
import zio.config.magnolia.deriveConfig

final case class AccessTokenIssuingConfig(
                                           issuer: String,
                                           audience: String,
                                           keyId: String,
                                           publicKeyPem: String,
                                           privateKeyPem: String,
                                           ttlSeconds: Long
                                         )

object AccessTokenIssuingConfig {
  given Config[AccessTokenIssuingConfig] = deriveConfig[AccessTokenIssuingConfig]
}
