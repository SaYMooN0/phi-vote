package backend.auth.endpoints

import backend.auth.*
import zio.http.{Request, Response}
import zio.json.ast.Json
import zio.json.{EncoderOps, JsonDecoder}
import zio.{IO, ZIO, http}

object SignUpEndpoint extends AppEndpoint {

  private final case class RawRequest(uniqueName: String, email: String, password: String) derives JsonDecoder

  private final case class Request(uniqueName: String, email: Email, password: String)

  override def handle(req: http.Request): IO[ResponseErr, Response] =
    for {
      signUpReq <- Request.from(req.body)
      response <- ZIO.succeed {
        Response.json(
          Json.Obj(
            "isOk" -> Json.Bool(true),
            "email" -> Json.Str(signUpReq.email.value)
          ).toJson
        )
      }
    } yield response


  private object Request extends ParsedRequestOf[RawRequest] {
    override type Parsed = Request

    override protected def fromRaw(req: RawRequest) = {
      val uniqueName = req.uniqueName.trim
      val emailRaw = req.email.trim

      val emailResult =
        Email.parse(emailRaw)

      val errors =
        List(
          Option.when(uniqueName.length < 3) {
            InvalidInputData(
              inputKey = "uniqueName",
              msg = "Unique name is too short",
              fixRec = Some("Use at least 3 characters")
            )
          },
          emailResult.left.toOption,
          Option.when(req.password.length < 8) {
            InvalidInputData(
              inputKey = "password",
              msg = "Password is too short",
              fixRec = Some("Use at least 8 characters")
            )
          }
        ).flatten

      (errors, emailResult) match {
        case (Nil, Right(email)) =>
          ZIO.succeed(
            Request(
              uniqueName = uniqueName,
              email = email,
              password = req.password
            )
          )

        case (first :: rest, _) =>
          ZIO.fail(new InvalidInputRespErr(first, rest *))

        case (Nil, Left(emailErr)) =>
          ZIO.fail(new InvalidInputRespErr(emailErr))
      }
    }
  }

}

