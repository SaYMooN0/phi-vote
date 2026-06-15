package backend.apishared.resp_errs

import zio.http.*
import zio.json.ast.Json

final case class InvalidInputData(inputKey: String, msg: String, fixRec: Option[String] = None)

final class InvalidInputRespErr(
                                 first: InvalidInputData,
                                 rest: InvalidInputData*
                               ) extends ResponseErr {

  override protected val errKey: String = "InvalidInput"

  override protected val status: Status = Status.BadRequest

  override def payload: Json.Obj = {
    def inputDataToJsonLine(item: InvalidInputData) =
      item.inputKey -> Json
        .Obj("errMsg" -> Json.Str(item.msg))
        .merge {
          item.fixRec match {
            case Some(value) => Json.Obj("fixRec" -> Json.Str(value))
            case None => Json.Obj()
          }
        }

    val all = first +: rest.toList
    val inputs = Json.Obj(all.map(inputDataToJsonLine) *)
    Json.Obj("inputs" -> inputs)
  }
}