package backend.authservice.api


import backend.authservice.api.endpoints.SignUpEndpoint
import backend.authservice.api.services.PasswordHashingServiceLive
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

        Quill.Postgres.fromNamingStrategy(io.getquill.SnakeCase),
        Quill.DataSource.fromPrefix("authServiceDb")
      )
  }

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.setConfigProvider(TypesafeConfigProvider.fromResourcePath())
}
