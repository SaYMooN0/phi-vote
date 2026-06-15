package backend.apishared.resp_errs

import zio.http.{Response, Status}
import zio.json.ast.Json
import zio.*
import zio.http.*
import zio.json.*
import zio.json.ast.Json

trait ResponseErr {

  protected def errKey: String

  protected def status: Status

  protected def payload: Json.Obj

  private final def toJson: String = Json
    .Obj(
      "isOk" -> Json.Bool(false),
      "errKey" -> Json.Str(errKey)
    )
    .merge(payload)
    .toJson


  final def toResponse: Response = Response.json(toJson).status(status)
}
