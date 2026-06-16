package backend.apishared

import backend.apishared.resp_errs.*
import zio.*
import zio.http.*

trait AppEndpoint {
  def handle(req: Request): IO[ResponseErr, Response]
}

trait EndpointProviderFor[E <: AppEndpoint](using Tag[E]) {

  final def handler: Handler[E, ResponseErr, Request, Response] =
    zio.http.handler { (req: Request) => ZIO.serviceWithZIO[E](_.handle(req)) }
}
