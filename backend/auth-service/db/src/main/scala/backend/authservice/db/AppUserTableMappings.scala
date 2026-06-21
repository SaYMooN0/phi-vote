package backend.authservice.db

import backend.authservice.domain.shared.*
import backend.dbshared.{MappedEncodingWithCrushIfCorrupted, QuillMappings}
import backend.domainshared.AppUserId
import io.getquill.MappedEncoding

import java.util.UUID

object AppUserTableMappings {
  given MappedEncoding[AppUserId, UUID] = QuillMappings.AppUserIdMappings.toUUID

  given MappedEncoding[UUID, AppUserId] = QuillMappings.AppUserIdMappings.fromUUID

  given MappedEncoding[String, Email] = MappedEncodingWithCrushIfCorrupted(Email.createFrom)

  given MappedEncoding[Email, String] = MappedEncoding[Email, String](_.value)

  given MappedEncoding[String, UserUniqueName] = MappedEncodingWithCrushIfCorrupted(UserUniqueName.createFrom)

  given MappedEncoding[UserUniqueName, String] = MappedEncoding[UserUniqueName, String](_.value)

  given MappedEncoding[PasswordHash, String] = MappedEncoding[PasswordHash, String](PasswordHash.value)

  given MappedEncoding[String, PasswordHash] = MappedEncoding[String, PasswordHash](PasswordHash.unsafeFrom)
}