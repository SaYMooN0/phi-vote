package backend.authservice.api.configs

import zio.Config
import zio.config.magnolia.deriveConfig


final case class EmailServiceConfig(
                                     sender: String,
                                     host: String,
                                     port: Int,
                                     user: String,
                                     pass: String
                                   )

object EmailServiceConfig {
  given Config[EmailServiceConfig] = deriveConfig[EmailServiceConfig]
}
