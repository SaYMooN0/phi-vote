package backend.auth

import zio.*
import zio.http.*
import zio.json.*
import zio.json.ast.Json


trait ResponseErr {

  protected def errKey: String;

  protected def status: Status;

  protected def payload: Json.Obj;

  private final def toJson: String = Json
    .Obj(
      "isOk" -> Json.Bool(false),
      "errKey" -> Json.Str(errKey)
    )
    .merge(payload)
    .toJson


  final def toResponse: Response = Response.json(toJson).status(status)
}

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