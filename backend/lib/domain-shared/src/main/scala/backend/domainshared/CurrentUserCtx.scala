package backend.domainshared

import backend.domainshared.AppUserId

import java.util.UUID

enum CurrentUserCtx {
  case Authenticated(userId: AppUserId)
  case KnownUnauthenticated(id: UnauthenticatedCtxId)
  case Anonymous
}

type UnauthenticatedCtxId = UnauthenticatedCtxId.Type

object UnauthenticatedCtxId {
  opaque type Type = UUID

  private def apply(value: UUID): Type = value

  def createNew(): Type = UUID.randomUUID()

  def createFrom(value: String): Option[Type] =
    scala.util.Try(UUID.fromString(value)).toOption.map(apply)

  extension (id: Type) {
    def value: UUID = id
  }
}
