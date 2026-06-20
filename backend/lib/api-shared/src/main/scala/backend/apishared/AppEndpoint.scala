package backend.apishared

import backend.apishared.resp_errs.*
import zio.*
import zio.http.*

trait AppEndpoint {
  def handle(httpReq: Request): IO[ResponseErr, Response]
}

trait EndpointProviderFor[E <: AppEndpoint](using Tag[E]) {

  final def handler: Handler[E, ResponseErr, Request, Response] =
    zio.http.handler { (r: Request) => ZIO.serviceWithZIO[E](_.handle(r)) }
}
