package backend.authservice.domain.services


import backend.authservice.Configs
import backend.authservice.domain.shared.PasswordHash
import de.mkammerer.argon2.Argon2Factory.Argon2Types
import de.mkammerer.argon2.{Argon2, Argon2Factory}
import zio.*
import zio.config.magnolia.deriveConfig

trait PasswordHashingService {
  def hash(password: UserPassword): Task[PasswordHash]

  def verify(passwordToVerify: UserPassword, hash: PasswordHash): Task[Boolean]
}

final case class PasswordHashingConfig(
                                        iterations: Int,
                                        memoryKiB: Int,
                                        parallelism: Int,
                                        saltLength: Int,
                                        hashLength: Int
                                      )

object PasswordHashingConfig {
  given Config[PasswordHashingConfig] = deriveConfig[PasswordHashingConfig]
}

object UserPassword {
  opaque type Type = String

  def unsafeFrom(value: String): Type = value

  extension (password: Type)
    private[authservice] def value: String = password
}

type UserPassword = UserPassword.Type
