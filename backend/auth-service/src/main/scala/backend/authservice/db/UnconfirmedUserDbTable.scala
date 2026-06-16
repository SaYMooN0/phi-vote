package backend.authservice.db

import backend.dbshared.DbTable

import java.time.Instant
import java.util.UUID

object UnconfirmedUserDbTable extends DbTable[UnconfirmedUser] {
  override val name: String = "unconfirmed_user"
}

final case class UnconfirmedUser(
                                  id: UUID,
                                  uniqueName: String,
                                  email: String,
                                  passwordHash: String,
                                  createdAt: Instant,
                                  updatedAt: Instant
                                )

