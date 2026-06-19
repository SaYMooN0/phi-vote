package backend.authservice.domain.services

import backend.authservice.domain.shared.PasswordHash
import zio.*

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


object UserPassword {
  opaque type Type = String

  def unsafeFrom(value: String): Type = value

  extension (password: Type)
    def value: String = password
}

type UserPassword = UserPassword.Type
