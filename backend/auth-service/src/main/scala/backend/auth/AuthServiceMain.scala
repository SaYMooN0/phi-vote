package backend.auth

import backend.shared.ServiceInfo
import zio.*
import zio.http.*

object AuthServiceMain extends ZIOAppDefault {

  private val routes =
    Routes(
      Method.GET / "health" -> handler(Response.text(ServiceInfo.health("auth-service")))
    )

  override def run: ZIO[Any, Throwable, Unit] =
    Server.serve(routes).provide(
      Server.defaultWithPort(8180)
    )
}