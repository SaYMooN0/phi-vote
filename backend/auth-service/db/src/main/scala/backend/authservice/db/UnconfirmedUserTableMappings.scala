package backend.authservice.db


import backend.authservice.domain.entities.UnconfirmedUserConfirmationToken
import backend.authservice.domain.shared.*
import backend.dbshared.MappedEncodingWithCrushIfCorrupted
import backend.domainshared.UnconfirmedUserId
import io.getquill.MappedEncoding

import java.util.UUID

object UnconfirmedUserTableMappings {
  given MappedEncoding[UnconfirmedUserId, UUID] = MappedEncoding[UnconfirmedUserId, UUID](UnconfirmedUserId.unwrap)

  given MappedEncoding[UUID, UnconfirmedUserId] = MappedEncoding[UUID, UnconfirmedUserId](UnconfirmedUserId(_))

  given MappedEncoding[String, Email] = MappedEncodingWithCrushIfCorrupted(Email.createFrom)

  given MappedEncoding[Email, String] = MappedEncoding[Email, String](_.value)

  given MappedEncoding[String, UserUniqueName] = MappedEncodingWithCrushIfCorrupted(UserUniqueName.createFrom)

  given MappedEncoding[UserUniqueName, String] = MappedEncoding[UserUniqueName, String](_.value)

  given MappedEncoding[PasswordHash, String] = MappedEncoding[PasswordHash, String](PasswordHash.value)

  given MappedEncoding[String, PasswordHash] = MappedEncoding[String, PasswordHash](PasswordHash.unsafeFrom)

  given MappedEncoding[UnconfirmedUserConfirmationToken, UUID] = MappedEncoding[UnconfirmedUserConfirmationToken, UUID](UnconfirmedUserConfirmationToken.unwrap)

  given MappedEncoding[UUID, UnconfirmedUserConfirmationToken] = MappedEncoding[UUID, UnconfirmedUserConfirmationToken](UnconfirmedUserConfirmationToken(_))

}