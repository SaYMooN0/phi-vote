package backend.auth

import backend.auth.endpoints.SignUpEndpoint
import zio.*
import zio.http.*
import zio.json.*
import zio.json.ast.Json

object AuthServiceMain extends ZIOAppDefault {
  private val routes: Routes[Any, ResponseErr] = {
    import scala.language.implicitConversions
    Routes(
      Method.POST / "sign-up" -> SignUpEndpoint
    )
  }

  private val httpRoutes: Routes[Any, Nothing] =
    routes.handleError(_.toResponse)

  override def run: ZIO[Any, Throwable, Unit] =
    Server
      .serve(httpRoutes)
      .provide(Server.defaultWithPort(8180))
}