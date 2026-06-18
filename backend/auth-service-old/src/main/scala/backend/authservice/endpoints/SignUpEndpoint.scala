package backend.authservice.endpoints

import backend.apishared.*
import backend.apishared.resp_errs.*
import backend.authservice.*
import backend.authservice.db.*
import backend.authservice.domain.entities.AppUser
import backend.authservice.domain.services.{PasswordHashingService, UserPassword}
import backend.authservice.domain.shared.Email
import backend.coreshared.*
import backend.dbshared.DbQuill
import com.github.f4b6a3.uuid.UuidCreator
import io.getquill.*
import zio.*
import zio.http.*
import zio.json.*

import scala.language.postfixOps

final class SignUpEndpoint private(
                                    db: DbQuill,
                                    passwordHashingService: PasswordHashingService
                                  ) extends AppEndpoint {

  import backend.dbshared.AppUserIdQuillMappings.given
  import backend.authservice.db.AuthDbMappings.given

  import db.*

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


  private def register(req: ParsedRequest): IO[ResponseErr, AppUserId] =
    (for {
      now <- Clock.instant

      passwordHash <- passwordHashingService.hash(
        UserPassword.unsafeFrom(req.password)
      )

      newUserId = AppUserId(UuidCreator.getTimeOrderedEpoch())

      appUser = AppUser(
        id = newUserId,
        uniqueName = req.uniqueName,
        email = req.email,
        passwordHash = passwordHash,
        createdAt = now,
        registrationDate = now
      )

      _ <- run {
        AppUserDbTable()
          .insertValue(lift(appUser))
      }.unit
    } yield newUserId)
      .tapError { err =>
        zio.Console.printLineError(
          s"[register] failed to insert app_user: ${err.getMessage}"
        ) *>
          ZIO.succeed(err.printStackTrace())
      }
      .mapError(_ => ??? : ResponseErr)

  private object RequestParser extends RequestParserFor[ParsedRequest, RawRequest] {

    override protected def fromRawToParsed(req: RawRequest) = {
      val uniqueName = req.uniqueName.trim
      val emailRaw = req.email.trim

      val emailResult: Either[InvalidInputData, Email] = Right(Email.unsafeFrom("email@em.com"))

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
    DbQuill & PasswordHashingService,
    Nothing,
    SignUpEndpoint
  ] =
    ZLayer.fromFunction {
      (
        quill: DbQuill,
        passwordHashingService: PasswordHashingService
      ) =>
        new SignUpEndpoint(quill, passwordHashingService)
    }
}