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
  protected type ErrContent = (String, Option[String])
  protected type InputErrOr[A] = Either[InvalidInputRespErr, A]

  def succeed[A](value: A): InputErrOr[A] =
    Right(value)

  def field[E, A](
                   inputKey: String,
                   result: Either[E, A]
                 )(
    leftToErrContent: E => ErrContent
                 ): InputErrOr[A] =
    result.left.map { err =>
      val (msg, fixRec) = leftToErrContent(err)
      InvalidInputRespErr.one(inputKey, msg, fixRec)
    }

  extension (firstErr: InvalidInputRespErr)
    private def merge(secondErr: InvalidInputRespErr): InvalidInputRespErr =
      InvalidInputRespErr.fromMap(firstErr.inputs ++ secondErr.inputs)

  extension [A](first: InputErrOr[A])
    def zipWithAccum[B, C](
                            second: InputErrOr[B]
                          )(
                            f: (A, B) => C
                          ): InputErrOr[C] =
      (first, second) match {
        case (Right(a), Right(b)) =>
          Right(f(a, b))

        case (Left(firstErr), Left(secondErr)) =>
          Left(firstErr.merge(secondErr))

        case (Left(err), Right(_)) =>
          Left(err)

        case (Right(_), Left(err)) =>
          Left(err)
      }

  def map2[A, B, C](
                     first: InputErrOr[A],
                     second: InputErrOr[B]
                   )(
                     f: (A, B) => C
                   ): InputErrOr[C] =
    first.zipWithAccum(second)(f)

  def map3[A, B, C, D](
                        first: InputErrOr[A],
                        second: InputErrOr[B],
                        third: InputErrOr[C]
                      )(
                        f: (A, B, C) => D
                      ): InputErrOr[D] =
    first
      .zipWithAccum(second)((_, _))
      .zipWithAccum(third) { case ((a, b), c) =>
        f(a, b, c)
      }
}