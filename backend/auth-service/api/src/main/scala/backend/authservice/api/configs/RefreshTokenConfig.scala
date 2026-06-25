package backend.authservice.api.configs

import zio.Config
import zio.config.magnolia.deriveConfig

final case class RefreshTokenConfig(
                                     ttlSeconds: Long,
                                     hmacSecret: String
                                   )

object RefreshTokenConfig {
  given Config[RefreshTokenConfig] = deriveConfig[RefreshTokenConfig]
}
