package backend.apishared.resp_errs


import backend.apishared.ResponseErr
import zio.http.Status
import zio.json.ast.Json

final class InternalServerErrorRespErr(extraDetails: Option[String] = None) extends ResponseErr {

  override protected val errKey: String = "InternalServerError"

  override protected val status: Status = Status.InternalServerError

  override protected val payload: Json.Obj =
    Json.Obj(
      "msg" -> Json.Str("Internal server error. Please try again later")
    ).merge {
      extraDetails match {
        case Some(value) => Json.Obj("details" -> Json.Str(value))
        case None => Json.Obj()
      }
    }
}

object InternalServerErrorRespErr {
  def DbOpFailed = InternalServerErrorRespErr(Some("Database operation failed"))
}