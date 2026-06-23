package backend.apishared.services

import backend.apishared.Configs
import backend.domainshared.{CurrentUserCtx, UnauthenticatedCtxId}
import zio.*
import zio.config.magnolia.deriveConfig
import zio.http.Request

final case class CurrentUserCtxConfig(
                                       accessTokenCookieName: String,
                                       unauthenticatedCtxCookieName: String
                                     )

object CurrentUserCtxConfig {
  given Config[CurrentUserCtxConfig] = deriveConfig[CurrentUserCtxConfig]
}

trait CurrentUserCtxService {
  def read(httpReq: Request): UIO[CurrentUserCtx]
}

private final class CurrentUserCtxServiceLive(
                                               config: CurrentUserCtxConfig,
                                               accessTokenReader: AccessTokenReader
                                             ) extends CurrentUserCtxService {


  override def read(httpReq: Request): UIO[CurrentUserCtx] = {
    def unauthenticatedCtx: CurrentUserCtx =
      cookieValue(config.unauthenticatedCtxCookieName, httpReq)
        .flatMap(UnauthenticatedCtxId.createFrom) match {
        case Some(id) => CurrentUserCtx.KnownUnauthenticated(id)
        case None => CurrentUserCtx.Anonymous
      }

    cookieValue(config.accessTokenCookieName, httpReq) match {
      case Some(accessToken) => {
        accessTokenReader
          .read(accessToken)
          .map(claims => CurrentUserCtx.Authenticated(claims.userId))
          .catchAll(_ => ZIO.succeed(unauthenticatedCtx))
      }
      case None => ZIO.succeed(unauthenticatedCtx)
    }

  }

  private def cookieValue(name: String, httpReq: Request): Option[String] =
    httpReq
      .cookies
      .find(_.name == name)
      .map(_.content)
      .filter(_.nonEmpty)
}

object CurrentUserCtxServiceLive {

  import CurrentUserCtxConfig.given

  val layer: ZLayer[
    CurrentUserCtxConfig & AccessTokenReader,
    Nothing,
    CurrentUserCtxService
  ] =
    ZLayer.fromFunction {
      (
        config: CurrentUserCtxConfig,
        accessTokenReader: AccessTokenReader
      ) =>
        new CurrentUserCtxServiceLive(config, accessTokenReader)
    }

  val configuredLayer: ZLayer[
    AccessTokenReader,
    Throwable,
    CurrentUserCtxService
  ] =
    Configs.makeLayer[CurrentUserCtxConfig]("currentUserCtx") >>> layer
}