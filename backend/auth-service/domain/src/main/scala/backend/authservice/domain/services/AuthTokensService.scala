package backend.authservice.domain.services

import backend.authservice.domain.entities.RefreshTokenPlain
import backend.domainshared.AppUserId
import zio.*

import java.time.Instant

trait AuthTokensService {
  def issueNewPair(userId: AppUserId): Task[IssuedAuthTokens]

  def refresh(refreshToken: RefreshTokenPlain): Task[IssuedAuthTokens]

  def revoke(refreshToken: RefreshTokenPlain): Task[Unit]
}

final case class IssuedAuthTokens(
  accessToken: IssuedAccessToken,
  refreshToken: RefreshTokenPlain,
  refreshTokenExpiresAt: Instant
)