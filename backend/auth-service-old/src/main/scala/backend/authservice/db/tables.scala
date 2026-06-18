package backend.authservice.db

import backend.authservice.domain.entities.*
import backend.coreshared.AppUserId
import backend.dbshared.DbTable

import java.time.Instant
import java.util.UUID

object AppUserDbTable extends DbTable[AppUser] {
  override inline val name: "app_user" = "app_user"
}

object UnconfirmedUserDbTable extends DbTable[UnconfirmedUser] {
  override inline val name: "unconfirmed_user" = "unconfirmed_user"
}




