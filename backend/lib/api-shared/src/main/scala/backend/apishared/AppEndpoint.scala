package backend.apishared

import backend.apishared.resp_errs.*
import zio.*
import zio.http.*

trait AppEndpoint[-R] {
  final def apply(): Handler[R, ResponseErr, Request, Response] = {
    handler { (req: Request) =>
      this.handle(req)
    }
  }

  def handle(req: Request): ZIO[R, ResponseErr, Response]
}