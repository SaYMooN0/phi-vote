package backend.authservice.domain.entities

import java.time.Instant
import java.util.UUID

final case class UnconfirmedUser(
                                  id: UUID,
                                  uniqueName: String,
                                  email: String,
                                  passwordHash: String,
                                  createdAt: Instant,
                                  updatedAt: Instant
                                )
