package backend.apishared

import zio.http.Response
import zio.json.*
import zio.json.ast.Json

object OkResponse {


  private val base: Json.Obj = Json.Obj(
    "isOk" -> Json.Bool(true)
  )

  def apply(): Response = Response.json(
    base.toJson
  )

  def apply(payload: Json.Obj): Response = Response.json(
    base.merge(payload).toJson
  )

  def apply[A: JsonEncoder](payload: A): Response =
    apply(payloadToJsonObj(payload))

  private def payloadToJsonObj[A: JsonEncoder](payload: A): Json.Obj =
    payload.toJson.fromJson[Json] match {
      case Right(obj: Json.Obj) => obj
      case Right(_) => throw new IllegalArgumentException("OkResponse(payload) expects payload to encode to a JSON object")
      case Left(err) => throw new IllegalArgumentException(s"Could not encode OkResponse payload as JSON object: $err")
    }
}