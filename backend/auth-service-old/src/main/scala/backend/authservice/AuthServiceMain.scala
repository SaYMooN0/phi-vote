package backend.authservice

import backend.authservice.endpoints.SignUpEndpoint
import backend.authservice.services.PasswordHashingServiceLive
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.*

object AuthServiceMain extends ZIOAppDefault {

  private val routes = Routes(
    Method.POST / "sign-up" -> SignUpEndpoint.handler
  )

  private val httpRoutes =
    routes.handleError(_.toResponse)

  override def run: ZIO[Any, Throwable, Unit] = {
    Server
      .serve(httpRoutes)
      .provide(
        Server.defaultWithPort(8180),

        SignUpEndpoint.live,
        PasswordHashingServiceLive.configuredLayer,

        Quill.Postgres.fromNamingStrategy(SnakeCase),
        Quill.DataSource.fromPrefix("authDb")
      )
  }

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.setConfigProvider(TypesafeConfigProvider.fromResourcePath())
}