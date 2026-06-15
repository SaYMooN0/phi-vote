package backend.authservice.db

import java.time.Instant
import java.util.UUID

object UnconfirmedUserDbTable extends DbTable {
  override val name: String = "unconfirmed_user"
  override protected type TableEntity = UnconfirmedUser

  final case class UnconfirmedUser(
                                    id: UUID,
                                    uniqueName: String,
                                    email: String,
                                    passwordHash: String,
                                    createdAt: Instant,
                                    updatedAt: Instant
                                  )
}