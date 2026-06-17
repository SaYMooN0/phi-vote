package backend.authservice.endpoints

import backend.apishared.*
import backend.apishared.resp_errs.*
import backend.authservice.*
import backend.authservice.db.AuthServiceDb
import backend.authservice.services.PasswordHashingService
import backend.coreshared.*
import zio.*
import zio.http.*
import zio.json.{JsonDecoder, JsonEncoder}


final class SignUpEndpoint private(
                                    db: AuthServiceDb,
                                    passwordHashingService: PasswordHashingService
                                  ) extends AppEndpoint {

  private final case class RawRequest(uniqueName: String, email: String, password: String)derives JsonDecoder

  private final case class ParsedRequest(uniqueName: String, email: Email, password: String)

  private final case class ResponseResult(email: String)derives JsonEncoder

  override def handle(req: Request): IO[ResponseErr, Response] =
    for {
      signUpReq <- RequestParser.parse(req.body)
      _ <- register(signUpReq)
    } yield OkResponse(ResponseResult(
      signUpReq.email.value
    ))


  private def register(req: ParsedRequest): IO[ResponseErr, Unit] = ZIO.unit

  private object RequestParser extends RequestParserFor[ParsedRequest, RawRequest] {

    override protected def fromRawToParsed(req: RawRequest) = {
      val uniqueName = req.uniqueName.trim
      val emailRaw = req.email.trim

      val emailResult: Either[InvalidInputData, Email] = Right(Email("Email"))

      val errors =
        List(
          Option.when(uniqueName.length < 3) {
            InvalidInputData(
              "uniqueName",
              "Unique name is too short",
              Some("Use at least 3 characters")
            )
          },
          emailResult.left.toOption,
          Option.when(req.password.length < 8) {
            InvalidInputData(
              "password",
              "Password is too short",
              Some("Use at least 8 characters")
            )
          }
        ).flatten

      (errors, emailResult) match {
        case (Nil, Right(email)) => ZIO.succeed(ParsedRequest(uniqueName, email, req.password))
        case (first :: rest, _) => ZIO.fail(new InvalidInputRespErr(first, rest *))
        case (Nil, Left(emailErr)) => ZIO.fail(new InvalidInputRespErr(emailErr))
      }
    }
  }
}

object SignUpEndpoint extends EndpointProviderFor[SignUpEndpoint] {

  val live: ZLayer[
    AuthServiceDb & PasswordHashingService,
    Nothing,
    SignUpEndpoint
  ] =
    ZLayer.fromFunction {
      (
        quill: AuthServiceDb,
        passwordHashingService: PasswordHashingService
      ) =>
        new SignUpEndpoint(quill, passwordHashingService)
    }
}