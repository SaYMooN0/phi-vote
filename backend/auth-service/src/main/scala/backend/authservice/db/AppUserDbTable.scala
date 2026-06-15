package backend.authservice.db

import java.time.Instant
import java.util.UUID

object AppUserDbTable extends DbTable {
  override val name: String = "app_user"
  override protected type TableEntity = AppUser

  case class AppUser(
                      id: UUID,
                      uniqueName: String,
                      email: String,
                      passwordHash: String,
                      createdAt: Instant,
                      registrationDate: Instant
                    )
}