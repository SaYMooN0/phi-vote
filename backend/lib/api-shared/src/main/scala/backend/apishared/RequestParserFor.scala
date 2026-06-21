package backend.apishared

import backend.apishared.resp_errs.*
import zio.http.Body
import zio.json.{DecoderOps, JsonDecoder}
import zio.{IO, ZIO}

trait RequestParserFor[Parsed, Raw: JsonDecoder] {

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

object RequestParserFor {
  private type ErrContent = (String, Option[String])
  private type InputErrOr[A] = Either[InvalidInputRespErr, A]
  private type InputField[E, A] = (String, (Either[E, A], E => ErrContent))

  private def parseField[E, A](field: InputField[E, A]): InputErrOr[A] = {
    val (inputKey, (result, leftToErrContent)) = field
    result.left.map { err =>
      val (msg, fixRec) = leftToErrContent(err)
      InvalidInputRespErr.one(inputKey, msg, fixRec)
    }
  }

  extension (firstErr: InvalidInputRespErr) {
    private def merge(secondErr: InvalidInputRespErr): InvalidInputRespErr =
      InvalidInputRespErr.merge(firstErr, secondErr)
  }
  extension [A](first: InputErrOr[A]) {
    private def zipWithAccum[B, R](
                                    second: InputErrOr[B]
                                  )(
                                    successConstructor: (A, B) => R
                                  ): InputErrOr[R] =
      (first, second) match {
        case (Right(a), Right(b)) => Right(successConstructor(a, b))
        case (Left(firstErr), Left(secondErr)) => Left(firstErr.merge(secondErr))
        case (Left(err), Right(_)) => Left(err)
        case (Right(_), Left(err)) => Left(err)
      }
  }

  def mapMultiple[E1, A, E2, B, R](
                                    first: InputField[E1, A],
                                    second: InputField[E2, B]
                                  )(
                                    successConstructor: (A, B) => R
                                  ): InputErrOr[R] =
    parseField(first).zipWithAccum(parseField(second))(successConstructor)

  def mapMultiple[E1, A, E2, B, E3, C, R](
                                           first: InputField[E1, A],
                                           second: InputField[E2, B],
                                           third: InputField[E3, C]
                                         )(
                                           successConstructor: (A, B, C) => R
                                         ): InputErrOr[R] =
    parseField(first)
      .zipWithAccum(parseField(second))((_, _))
      .zipWithAccum(parseField(third)) { case ((a, b), c) => successConstructor(a, b, c) }

  def finish[A](result: InputErrOr[A]): IO[InvalidInputRespErr, A] = ZIO.fromEither(result)
}