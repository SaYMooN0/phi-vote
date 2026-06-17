package backend.authservice.db

import backend.dbshared.DbTable

import java.time.Instant
import java.util.UUID

object AppUserDbTable extends DbTable[AppUserDb] {
  override val name: String = "app_user"
}

case class AppUserDb(
                      id: UUID, //make opaque
                      uniqueName: String,
                      email: String,
                      passwordHash: String,
                      createdAt: Instant,
                      registrationDate: Instant
                    )
