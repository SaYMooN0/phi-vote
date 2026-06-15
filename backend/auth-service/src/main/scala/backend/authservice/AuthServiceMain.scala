package backend.authservice

import backend.apishared.resp_errs.*
import backend.authservice.endpoints.SignUpEndpoint
import backend.authservice.services
import backend.authservice.services.PasswordHashingServiceLive
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.*
import zio.http.*

import scala.language.postfixOps

object AuthServiceMain extends ZIOAppDefault {
  private val routes: Routes[Quill.Postgres[SnakeCase], ResponseErr] = {
    Routes(
      Method.POST / "sign-up" -> SignUpEndpoint()
    )
  }

  private val httpRoutes: Routes[Quill.Postgres[SnakeCase], Nothing] =
    routes.handleError(_.toResponse)

  override def run: ZIO[Any, Throwable, Unit] =
    Server
      .serve(httpRoutes)
      .provide(
        Server.defaultWithPort(8180),
        Quill.Postgres.fromNamingStrategy(SnakeCase),
        Quill.DataSource.fromPrefix("authDb"),
        PasswordHashingServiceLive.configuredLayer,
      )
}