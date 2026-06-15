package backend.apishared

import backend.apishared.resp_errs.*
import zio.http.Body
import zio.json.{DecoderOps, JsonDecoder}
import zio.{IO, ZIO}

trait RequestParserFor[Raw: JsonDecoder] {
  type Parsed

  protected def fromRawToParsed(req: Raw): IO[InvalidInputRespErr, Parsed]

  final def parse(reqBody: Body): IO[ResponseErr, Parsed] = for {
    body <- reqBody
      .asString
      .mapError(_ => MalformedJsonRespErr("Could not read request body"))

    raw <- ZIO
      .fromEither(body.fromJson[Raw])
      .mapError(MalformedJsonRespErr(_))

    parsed <- fromRawToParsed(raw)
  } yield parsed
}

