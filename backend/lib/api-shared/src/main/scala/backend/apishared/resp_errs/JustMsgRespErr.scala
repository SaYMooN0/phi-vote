package backend.apishared.resp_errs

import backend.apishared.ResponseErr
import zio.http.Status
import zio.json.ast.Json

class JustMsgRespErr(msg: String, override protected val status: Status) extends ResponseErr {

  override protected val errKey: String = "JustMsg"
  override protected val payload: Json.Obj =
    Json.Obj(
      "msg" -> Json.Str(msg)
    )

}
