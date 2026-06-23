package backend.authservice.db

import backend.authservice.domain.entities.{RefreshTokenHash, RefreshTokenSession, RefreshTokenSessionId}
import backend.dbshared.QuillMappings
import backend.domainshared.AppUserId
import io.getquill.{EntityQuery, MappedEncoding, Quoted, querySchema, quote}

import java.util.UUID

object RefreshTokenSessionDbTable {
  inline def apply(): Quoted[EntityQuery[RefreshTokenSession]] = {
    quote(querySchema[RefreshTokenSession]("refresh_token_session"))
  }
}

object RefreshTokenSessionTableMappings {

  given MappedEncoding[RefreshTokenSessionId, UUID] = MappedEncoding[RefreshTokenSessionId, UUID](_.value)

  given MappedEncoding[UUID, RefreshTokenSessionId] = MappedEncoding[UUID, RefreshTokenSessionId](RefreshTokenSessionId.apply)

  given MappedEncoding[RefreshTokenHash, String] = MappedEncoding[RefreshTokenHash, String](_.value)

  given MappedEncoding[String, RefreshTokenHash] = MappedEncoding[String, RefreshTokenHash](RefreshTokenHash.unsafeFrom)

  given MappedEncoding[AppUserId, UUID] = QuillMappings.AppUserIdMappings.toUUID

  given MappedEncoding[UUID, AppUserId] = QuillMappings.AppUserIdMappings.fromUUID
}