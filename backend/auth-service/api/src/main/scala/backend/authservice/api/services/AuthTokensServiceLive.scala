package backend.authservice.api.services

import backend.apishared.Configs
import backend.authservice.domain.services.{AccessTokenIssuer, IssuedAccessToken, RefreshTokenSecurityService}
import backend.dbshared.DbQuill
import backend.domainshared.AppUserId
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.RSAKeyProvider
import zio.*
import zio.config.magnolia.deriveConfig

import java.security.KeyFactory
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.time.Instant
import java.util.{Base64, Date}
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

  override def issueNewPair(userId: AppUserId): Task[IssuedAuthTokens] =
    for {
      now <- Clock.instant

      accessToken <- accessTokenIssuer.issueFor(userId)

      refreshToken <- refreshTokenSecurityService.generate
      refreshTokenHash <- refreshTokenSecurityService.hash(refreshToken)

      refreshTokenExpiresAt = now.plusSeconds(refreshTokenConfig.ttlSeconds)

      session = RefreshTokenSession(
        id = RefreshTokenSessionId.createNew(),
        userId = userId,
        tokenHash = refreshTokenHash,
        createdAt = now,
        expiresAt = refreshTokenExpiresAt,
        revokedAt = None,
        replacedBySessionId = None
      )

      _ <- run {
        RefreshTokenSessionDbTable()
          .insertValue(lift(session))
      }
    } yield IssuedAuthTokens(
      accessToken = accessToken,
      refreshToken = refreshToken,
      refreshTokenExpiresAt = refreshTokenExpiresAt
    )

  override def refresh(refreshToken: RefreshTokenPlain): IO[RefreshTokenErr, IssuedAuthTokens] =
    for {
      now <- Clock.instant

      refreshTokenHash <- refreshTokenSecurityService
        .hash(refreshToken)
        .mapError(_ => RefreshTokenErr.NotFound)

      existingSessionOpt <- run {
        RefreshTokenSessionDbTable()
          .filter(_.tokenHash == lift(refreshTokenHash))
          .take(1)
      }
        .map(_.headOption)
        .mapError(_ => RefreshTokenErr.NotFound)

      existingSession <- ZIO
        .fromOption(existingSessionOpt)
        .orElseFail(RefreshTokenErr.NotFound)

      result <-
        if existingSession.revokedAt.nonEmpty then
          revokeAllForUser(existingSession.userId, now)
            .as(RefreshTokenErr.ReuseDetected)
            .flip

        else if !existingSession.expiresAt.isAfter(now) then
          ZIO.fail(RefreshTokenErr.Expired)

        else
          rotateActiveSession(existingSession, now)
    } yield result

  override def revoke(refreshToken: RefreshTokenPlain): Task[Unit] =
    for {
      now <- Clock.instant

      refreshTokenHash <- refreshTokenSecurityService.hash(refreshToken)

      _ <- run {
        RefreshTokenSessionDbTable()
          .filter(session =>
            session.tokenHash == lift(refreshTokenHash) &&
              session.revokedAt.isEmpty
          )
          .update(
            _.revokedAt -> lift(Some(now))
          )
      }
    } yield ()

  private def rotateActiveSession(
                                   existingSession: RefreshTokenSession,
                                   now: Instant
                                 ): IO[RefreshTokenErr, IssuedAuthTokens] =
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

      newRefreshTokenExpiresAt =
        now.plusSeconds(refreshTokenConfig.ttlSeconds)

      newSession = RefreshTokenSession(
        id = RefreshTokenSessionId.createNew(),
        userId = existingSession.userId,
        tokenHash = newRefreshTokenHash,
        createdAt = now,
        expiresAt = newRefreshTokenExpiresAt,
        revokedAt = None,
        replacedBySessionId = None
      )

      _ <- transaction {
        for {
          updatedCount <- run {
            RefreshTokenSessionDbTable()
              .filter(session =>
                session.id == lift(existingSession.id) &&
                  session.revokedAt.isEmpty
              )
              .update(
                _.revokedAt -> lift(Some(now)),
                _.replacedBySessionId -> lift(Some(newSession.id))
              )
          }

          _ <-
            if updatedCount == 1 then ZIO.unit
            else ZIO.fail(RuntimeException("Refresh token session is already revoked or rotated"))

          _ <- run {
            RefreshTokenSessionDbTable()
              .insertValue(lift(newSession))
          }
        } yield ()
      }.mapError(_ => RefreshTokenErr.Revoked)
    } yield IssuedAuthTokens(
      newAccessToken,
      newRefreshToken,
      newRefreshTokenExpiresAt
    )

  private def revokeAllForUser(
                                userId: AppUserId,
                                now: Instant
                              ): IO[RefreshTokenErr, Unit] =
    run {
      RefreshTokenSessionDbTable()
        .filter(session =>
          session.userId == lift(userId) &&
            session.revokedAt.isEmpty
        )
        .update(
          _.revokedAt -> lift(Some(now))
        )
    }
      .unit
      .mapError(_ => RefreshTokenErr.Revoked)
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