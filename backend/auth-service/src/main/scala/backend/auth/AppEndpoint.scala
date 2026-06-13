package backend.auth

import zio.IO
import zio.http.{Handler, Request, Response, handler}

trait AppEndpoint {
  def handle(req: Request): IO[ResponseErr, Response]

  final def toHandler: Handler[Any, ResponseErr, Request, Response] =
    handler { (req: Request) =>
      handle(req)
    }
}
object AppEndpoint {
  given Conversion[AppEndpoint, Handler[Any, ResponseErr, Request, Response]] with
    override def apply(appEndpoint: AppEndpoint): Handler[Any, ResponseErr, Request, Response] =
      handler { (req: Request) =>
        appEndpoint.handle(req)
      }
}