package backend.authservice.api.services

import backend.authservice.api.configs.RefreshTokenConfig
import backend.authservice.db.RefreshTokenSessionDbTable
import backend.authservice.domain.entities.*
import backend.authservice.domain.services.*
import backend.dbshared.DbQuill
import backend.domainshared.AppUserId
import io.getquill.*
import zio.*

import java.time.Instant

private final class AuthTokensServiceLive private(
  db: DbQuill,
  accessTokenIssuer: AccessTokenIssuer,
  refreshTokenSecurityService: RefreshTokenSecurityService,
  refreshTokenConfig: RefreshTokenConfig
) extends AuthTokensService {

  import backend.authservice.db.RefreshTokenSessionTableMappings.given
  import db.*

  override def issueNewPair(userId: AppUserId): Task[IssuedAuthTokens] = {
    for {
      now <- Clock.instant
      accessToken <- accessTokenIssuer.issueFor(userId)
      refreshToken <- refreshTokenSecurityService.generate
      refreshTokenHash <- refreshTokenSecurityService.hash(refreshToken)
      refreshTokenExpiresAt = now.plusSeconds(refreshTokenConfig.ttlSeconds)

      session = RefreshTokenSession(
        RefreshTokenSessionId.createNew(),
        userId, refreshTokenHash, now, refreshTokenExpiresAt,
        revokedAt = None
      )

      _ <- insertNewSession(session)
    } yield IssuedAuthTokens(accessToken, refreshToken, refreshTokenExpiresAt)
  }

  override def refresh(refreshToken: RefreshTokenPlain): Task[IssuedAuthTokens] = {
    for {
      now <- Clock.instant
      refreshTokenHash <- refreshTokenSecurityService.hash(refreshToken)
      existingSession <- findSessionByHash(refreshTokenHash)
        .someOrFail(Exception("Refresh token session not found"))

      result <-
        if (existingSession.revokedAt.nonEmpty) {
          revokeAllActiveSessionsForUser(existingSession.userId, now) *>
            ZIO.fail(Exception("Refresh token reuse detected"))
        } else if (!existingSession.expiresAt.isAfter(now)) {
          ZIO.fail(Exception("Refresh token expired"))
        } else {
          rotateActiveSession(existingSession, now)
        }
    } yield result
  }

  override def revoke(refreshToken: RefreshTokenPlain): Task[Unit] = {
    for {
      now <- Clock.instant
      refreshTokenHash <- refreshTokenSecurityService.hash(refreshToken)
      _ <- revokeActiveSessionByHash(refreshTokenHash, now)
    } yield ()
  }

  private def rotateActiveSession(
    existingSession: RefreshTokenSession,
    now: Instant
  ): Task[IssuedAuthTokens] = {
    for {
      newAccessToken <- accessTokenIssuer.issueFor(existingSession.userId)
      newRefreshToken <- refreshTokenSecurityService.generate
      newRefreshTokenHash <- refreshTokenSecurityService.hash(newRefreshToken)
      newRefreshTokenExpiresAt = now.plusSeconds(refreshTokenConfig.ttlSeconds)

      newSession = RefreshTokenSession(
        RefreshTokenSessionId.createNew(),
        existingSession.userId, newRefreshTokenHash,
        createdAt = now,
        expiresAt = newRefreshTokenExpiresAt,
        revokedAt = None
      )

      _ <- transaction {
        for {
          updatedCount <- revokeExistingSession(existingSession.id, now)

          _ <-
            if (updatedCount == 1) ZIO.unit
            else ZIO.fail(RuntimeException("Refresh token session is already revoked or rotated"))

          _ <- insertNewSession(newSession)
        } yield ()
      }
    } yield IssuedAuthTokens(newAccessToken, newRefreshToken, newRefreshTokenExpiresAt)
  }

  private def revokeAllForUser(userId: AppUserId, now: Instant): Task[Unit] = {
    revokeAllActiveSessionsForUser(userId, now).unit
  }

  private def insertNewSession(newSession: RefreshTokenSession): Task[Unit] = {
    run {
      quote {
        RefreshTokenSessionDbTable()
          .insertValue(lift(newSession))
      }
    }.unit
  }

  private def findSessionByHash(tokenHash: RefreshTokenHash): Task[Option[RefreshTokenSession]] = {
    run {
      quote {
        RefreshTokenSessionDbTable()
          .filter(_.tokenHash == lift(tokenHash))
          .take(1)
      }
    }
      .map(_.headOption)
  }

  private def revokeActiveSessionByHash(tokenHash: RefreshTokenHash, now: Instant): Task[Unit] = {
    run {
      quote {
        RefreshTokenSessionDbTable()
          .filter(session => session.tokenHash == lift(tokenHash) && session.revokedAt.isEmpty)
          .update(_.revokedAt -> lift(Option(now)))
      }
    }.unit
  }

  private def revokeExistingSession(id: RefreshTokenSessionId, now: Instant): Task[Long] = {
    run {
      quote {
        RefreshTokenSessionDbTable()
          .filter(session => session.id == lift(id) && session.revokedAt.isEmpty)
          .update(_.revokedAt -> lift(Option(now)))
      }
    }
  }

  private def revokeAllActiveSessionsForUser(userId: AppUserId, now: Instant): Task[Long] = {
    run {
      quote {
        RefreshTokenSessionDbTable()
          .filter(session => session.userId == lift(userId) && session.revokedAt.isEmpty)
          .update(_.revokedAt -> lift(Option(now)))
      }
    }
  }
}

object AuthTokensServiceLive {

  val layer: ZLayer[
    DbQuill &
      AccessTokenIssuer &
      RefreshTokenSecurityService &
      RefreshTokenConfig,
    Nothing,
    AuthTokensService
  ] =
    ZLayer.fromFunction {
      (
        db: DbQuill,
        accessTokenIssuer: AccessTokenIssuer,
        refreshTokenSecurityService: RefreshTokenSecurityService,
        refreshTokenConfig: RefreshTokenConfig
      ) =>
        new AuthTokensServiceLive(
          db,
          accessTokenIssuer,
          refreshTokenSecurityService,
          refreshTokenConfig
        )
    }
}