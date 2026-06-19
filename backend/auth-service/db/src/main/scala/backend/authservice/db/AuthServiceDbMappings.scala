package backend.authservice.db

import backend.authservice.domain.shared.{Email, PasswordHash}
import io.getquill.MappedEncoding

object AuthServiceDbMappings {
  given MappedEncoding[Email, String] =
    MappedEncoding[Email, String](Email.unwrap)

  given MappedEncoding[String, Email] =
    MappedEncoding[String, Email](Email.unsafeFrom)

  given MappedEncoding[PasswordHash, String] =
    MappedEncoding[PasswordHash, String](PasswordHash.value)

  given MappedEncoding[String, PasswordHash] =
    MappedEncoding[String, PasswordHash](PasswordHash.unsafeFrom)
}