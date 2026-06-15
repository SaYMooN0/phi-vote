package backend.voting

import zio.*
import zio.http.*

object VotingServiceMain extends ZIOAppDefault {

  private val routes =
    Routes(
      Method.GET / "health" ->
        handler(Response.text(ServiceInfo.health("voting-service")))
    )

  override def run: ZIO[Any, Throwable, Unit] =
    Server.serve(routes).provide(
      Server.defaultWithPort(8181)
    )
}