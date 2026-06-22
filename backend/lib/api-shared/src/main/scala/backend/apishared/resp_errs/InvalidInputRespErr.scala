package backend.apishared.resp_errs

import backend.apishared.ResponseErr
import zio.http.*
import zio.json.ast.Json

final class InvalidInputRespErr private(
                                         private val inputs: Map[String, (String, Option[String])] //input key -> (msg, fix rec)
                                       ) extends ResponseErr {

  override protected val errKey: String = "InvalidInput"

  override protected val status: Status = Status.BadRequest

  override def payload: Json.Obj = {
    val inputsJson = Json.Obj(
      inputs.toList.map {
        case (inputKey, (msg, fixRec)) =>
          inputKey -> Json
            .Obj("errMsg" -> Json.Str(msg))
            .merge {
              fixRec match {
                case Some(value) => Json.Obj("fixRec" -> Json.Str(value))
                case None => Json.Obj()
              }
            }
      } *
    )

    Json.Obj("inputs" -> inputsJson)
  }
}

object InvalidInputRespErr {
  def fromMap(
               inputs: Map[String, (String, Option[String])]
             ): InvalidInputRespErr =
    new InvalidInputRespErr(inputs)

  def one(inputKey: String, msg: String, fixRec: Option[String] = None): InvalidInputRespErr =
    new InvalidInputRespErr(Map(inputKey -> (msg, fixRec)))

  def field(inputKey: String, msg: String, fixRec: Option[String] = None): (String, (String, Option[String])) =
    inputKey -> (msg, fixRec)

  def merge(first: InvalidInputRespErr, second: InvalidInputRespErr): InvalidInputRespErr =
    new InvalidInputRespErr(first.inputs ++ second.inputs)
}