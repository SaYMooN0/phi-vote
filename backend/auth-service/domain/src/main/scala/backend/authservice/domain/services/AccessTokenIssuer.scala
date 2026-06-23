package backend.authservice.domain.services

import backend.domainshared.AppUserId
import zio.Task

import java.time.Instant


trait AccessTokenIssuer {
  def issueFor(userId: AppUserId): Task[IssuedAccessToken]
}

final case class IssuedAccessToken(value: String, expiresAt: Instant)
