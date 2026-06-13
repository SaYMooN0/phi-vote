package backend.auth

import backend.auth.{InvalidInputRespErr, ResponseErr}
import zio.http.{Body, Status}
import zio.json.ast.Json
import zio.json.{DecoderOps, JsonDecoder}
import zio.{IO, ZIO}

private trait ParsedRequestOf[Raw: JsonDecoder] {
  type Parsed

  protected def fromRaw(req: Raw): IO[InvalidInputRespErr, Parsed]

  final def from(reqBody: Body): IO[ResponseErr, Parsed] =
    for {
      body <- reqBody.asString.mapError { _ =>
        MalformedJsonRespErr("Could not read request body"): ResponseErr
      }

      raw <- ZIO.fromEither(body.fromJson[Raw]).mapError { jsonErr =>
        MalformedJsonRespErr(jsonErr): ResponseErr
      }

      parsed <- fromRaw(raw).mapError(err => err: ResponseErr)
    } yield parsed
}

final class MalformedJsonRespErr(message: String) extends ResponseErr {

  override protected val errKey: String = "MalformedJson"

  override protected val status: Status = Status.BadRequest

  override protected val payload: Json.Obj = Json.Obj(
    "msg" -> Json.Str(message)
  )
}