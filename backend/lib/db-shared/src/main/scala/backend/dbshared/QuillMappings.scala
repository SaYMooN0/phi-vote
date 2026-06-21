package backend.dbshared

import backend.domainshared.AppUserId
import io.getquill.MappedEncoding

import java.util.UUID

object QuillMappings {
  object AppUserIdMappings {
    val toUUID: MappedEncoding[AppUserId, UUID] = MappedEncoding[AppUserId, UUID](AppUserId.unwrap)
    val fromUUID: MappedEncoding[UUID, AppUserId] = MappedEncoding[UUID, AppUserId](AppUserId(_))
  }
}