package backend.coreshared
import java.util.UUID

object AppUserId {
  opaque type Type = UUID

  def apply(value: UUID): Type =
    value

  def unwrap(id: Type): UUID =
    id

  extension (id: Type)
    def value: UUID = unwrap(id)
}

type AppUserId = AppUserId.Type