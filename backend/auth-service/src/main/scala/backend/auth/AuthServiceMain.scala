package backend.auth

import zio.*
import zio.http.*
import zio.json.*

object AuthServiceMain extends ZIOAppDefault {
  final case class HealthResponse(service: String, status: String) derives JsonEncoder

  private def rootResponse(r: Request): ZIO[Any, Throwable, Response] =
    ZIO.succeed {
      val body = HealthResponse(
        service = "auth-service",
        status = "ok"
      )

      InvalidInputRespErr(
        InvalidInputData("email", "no @ found")
      ).toResponse
    }

  private val rootRoutes: Routes[Any, Throwable] =
    Routes(
      Method.GET / Root -> handler {
        rootResponse
      }
    )

  private val userRoutes: Routes[Any, Throwable] =
    Routes(
      Method.GET / "user" / "me" -> handler {
        Response.json("""{"id":"123","name":"John"}""")
      },

      Method.POST / "user" / "login" -> handler { (_: Request) =>
        ZIO.succeed {
          Response.json("""{"token":"fake-token"}""")
        }
      }
    )

  private val notFoundRoutes: Routes[Any, Throwable] =
    Routes(
      Method.ANY / trailing -> handler { (req: Request) =>
        Response
          .json(s"""{"error":"NotFound","path":"${req.url.path.encode}"}""")
          .status(Status.NotFound)
      }
    )

  private val routes: Routes[Any, Throwable] =
    rootRoutes ++ userRoutes ++ notFoundRoutes

  private val routesWithErrorsHandled: Routes[Any, Response] =
    routes.handleErrorCause { cause =>
      Response
        .json(s"""{"error":"InternalServerError","message":"${cause.prettyPrint}"}""")
        .status(Status.InternalServerError)
    }

  override def run: ZIO[Any, Throwable, Unit] =
    Server
      .serve(routesWithErrorsHandled)
      .provide(Server.defaultWithPort(8180))
}