package backend.authservice.api.services

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
        RefreshTokenSessionId.createNew(), userId, refreshTokenHash, now, refreshTokenExpiresAt,
        revokedAt = None
      )
      insertSessionQuery = quote {
        RefreshTokenSessionDbTable().insertValue(lift(session))
      }
      _ <- run(insertSessionQuery).unit
    } yield IssuedAuthTokens(accessToken, refreshToken, refreshTokenExpiresAt)
  }

  override def refresh(refreshToken: RefreshTokenPlain): IO[RefreshTokenErr, IssuedAuthTokens] = {
    for {
      now <- Clock.instant

      refreshTokenHash <- refreshTokenSecurityService
        .hash(refreshToken)
        .mapError(_ => RefreshTokenErr.NotFound)

      findSessionByHashQuery = quote {
        RefreshTokenSessionDbTable()
          .filter(_.tokenHash == lift(refreshTokenHash))
          .take(1)
      }

      existingSessionOpt <- run(findSessionByHashQuery)
        .map(_.headOption)
        .mapError(_ => RefreshTokenErr.NotFound)

      existingSession <- ZIO
        .fromOption(existingSessionOpt)
        .orElseFail(RefreshTokenErr.NotFound)

      result <-
        if (existingSession.revokedAt.nonEmpty) {
          revokeAllForUser(existingSession.userId, now)
            *> ZIO.fail(RefreshTokenErr.ReuseDetected)
        } else if (!existingSession.expiresAt.isAfter(now)) {
          ZIO.fail(RefreshTokenErr.Expired)
        } else {
          rotateActiveSession(existingSession, now)
        }
    } yield result
  }

  override def revoke(refreshToken: RefreshTokenPlain): Task[Unit] = {
    for {
      now <- Clock.instant
      refreshTokenHash <- refreshTokenSecurityService.hash(refreshToken)
      revokeByHashQuery = quote {
        RefreshTokenSessionDbTable()
          .filter(session => session.tokenHash == lift(refreshTokenHash) && session.revokedAt.isEmpty)
          .update(_.revokedAt -> lift(Option(now)))
      }
      _ <- run(revokeByHashQuery).unit
    } yield ()
  }

  private def rotateActiveSession(existingSession: RefreshTokenSession, now: Instant): IO[RefreshTokenErr, IssuedAuthTokens] =
    for {
      newAccessToken <- accessTokenIssuer
        .issueFor(existingSession.userId)
        .mapError(_ => RefreshTokenErr.NotFound)

      newRefreshToken <- refreshTokenSecurityService
        .generate
        .mapError(_ => RefreshTokenErr.NotFound)

      newRefreshTokenHash <- refreshTokenSecurityService
        .hash(newRefreshToken)
        .mapError(_ => RefreshTokenErr.NotFound)

      newRefreshTokenExpiresAt = now.plusSeconds(refreshTokenConfig.ttlSeconds)

      newSession = RefreshTokenSession(
        RefreshTokenSessionId.createNew(),
        existingSession.userId, newRefreshTokenHash,
        now, newRefreshTokenExpiresAt,
        revokedAt = None
      )

      _ <- transaction {
        for {

          updatedCount <- run(
            revokeExistingSessionQuery(
              existingSession.id,
              Option(now)
            )
          )

          _ <-
            if updatedCount == 1 then ZIO.unit
            else ZIO.fail(RuntimeException("Refresh token session is already revoked or rotated"))

          insertNewSessionQuery =
            RefreshTokenSessionDbTable()
              .insertValue(lift(newSession))

          _ <- run(quote(insertNewSessionQuery)).unit
        } yield ()
      }.mapError(_ => RefreshTokenErr.Revoked)
    } yield IssuedAuthTokens(newAccessToken, newRefreshToken, newRefreshTokenExpiresAt)

  private inline def revokeExistingSessionQuery(
                                                 sessionId: RefreshTokenSessionId,
                                                 revokedAt: Option[Instant]
                                               ) =
    quote {
      RefreshTokenSessionDbTable()
        .filter(session =>
          session.id == lift(sessionId) &&
            session.revokedAt.isEmpty
        )
        .update(
          _.revokedAt -> lift(revokedAt)
        )
    }
  private def revokeAllForUser(userId: AppUserId, now: Instant): IO[RefreshTokenErr, Unit] = {
    for {
      revokeAllForUserQuery = quote {
        RefreshTokenSessionDbTable()
          .filter(session => session.userId == lift(userId) && session.revokedAt.isEmpty)
          .update(_.revokedAt -> lift(Option(now)))
      }

      _ <- run(revokeAllForUserQuery)
        .unit
        .mapError(_ => RefreshTokenErr.Revoked)
    } yield ()
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