package backend.authservice.domain.entities

import backend.domainshared.{AppUserId, UuidWrapperCompanion}

import java.time.Instant

final case class RefreshTokenSession(
                                      id: RefreshTokenSessionId,
                                      userId: AppUserId,
                                      tokenHash: RefreshTokenHash,
                                      createdAt: Instant,
                                      expiresAt: Instant,
                                      revokedAt: Option[Instant],
                                      replacedBySessionId: Option[RefreshTokenSessionId]
                                    ) {
  def isActiveAt(now: Instant): Boolean =
    revokedAt.isEmpty && expiresAt.isAfter(now)
}

enum RefreshTokenErr {
  case Missing
  case NotFound
  case Expired
  case Revoked
  case ReuseDetected
}

object RefreshTokenSessionId extends UuidWrapperCompanion

type RefreshTokenSessionId = RefreshTokenSessionId.Type

type RefreshTokenPlain = RefreshTokenPlain.Type

object RefreshTokenPlain {
  opaque type Type = String

  def unsafeFrom(value: String): Type = value

  extension (token: Type) {
    def value: String = token
  }
}

type RefreshTokenHash = RefreshTokenHash.Type

object RefreshTokenHash {
  opaque type Type = String

  def unsafeFrom(value: String): Type = value

  extension (hash: Type) {
    def value: String = hash
  }
}
