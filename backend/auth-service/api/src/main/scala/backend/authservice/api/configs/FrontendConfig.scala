package backend.authservice.api.configs

import zio.Config
import zio.config.magnolia.deriveConfig

final case class FrontendConfig(
                                 baseUrl: String,
                                 confirmRegistrationPath: String
                               )


object FrontendConfig {
  given Config[FrontendConfig] = deriveConfig[FrontendConfig]
}
