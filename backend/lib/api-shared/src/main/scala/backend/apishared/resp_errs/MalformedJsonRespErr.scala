package backend.apishared.resp_errs

import zio.http.Status
import zio.json.ast.Json

final class MalformedJsonRespErr(message: String) extends ResponseErr {

  override protected val errKey: String = "MalformedJson"

  override protected val status: Status = Status.BadRequest

  override protected val payload: Json.Obj = Json.Obj(
    "msg" -> Json.Str(message)
  )
}