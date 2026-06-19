package backend.dbshared

import backend.domainshared.AppUserId
import io.getquill.MappedEncoding

import java.util.UUID

object AppUserIdQuillMappings {

  given MappedEncoding[AppUserId, UUID] =
    MappedEncoding[AppUserId, UUID](AppUserId.unwrap)

  given MappedEncoding[UUID, AppUserId] =
    MappedEncoding[UUID, AppUserId](AppUserId(_))
}