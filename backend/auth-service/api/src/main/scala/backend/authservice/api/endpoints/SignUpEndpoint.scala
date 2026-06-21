package backend.authservice.api.endpoints

import backend.apishared.*
import backend.apishared.resp_errs.*
import backend.authservice.db.AppUserDbTable
import backend.authservice.domain.entities.AppUser
import backend.authservice.domain.services.{PasswordHashingService, UserPassword, UserPasswordCreationErr}
import backend.authservice.domain.shared.{Email, EmailCreationErr, UserUniqueName, UserUniqueNameCreationErr}
import backend.dbshared.DbQuill
import backend.domainshared.AppUserId
import com.github.f4b6a3.uuid.UuidCreator
import io.getquill.*
import zio.*
import zio.http.*
import zio.json.{JsonDecoder, JsonEncoder}

final class SignUpEndpoint private(
                                    db: DbQuill,
                                    passwordHashingService: PasswordHashingService
                                  ) extends AppEndpoint {

  import backend.authservice.db.AppUserTableMappings.given
  import db.*

  private final case class RawRequest(uniqueName: String, email: String, password: String)derives JsonDecoder

  private final case class ParsedRequest(uniqueName: UserUniqueName, email: Email, password: UserPassword)

  private final case class ResponseResult(email: String)derives JsonEncoder

  override def handle(httpReq: Request): IO[ResponseErr, Response] =
    for {
      req <- RequestParser.parse(httpReq.body)
      _ <- register(req)
    } yield OkResponse(ResponseResult(
      req.email.value
    ))


  private def register(req: ParsedRequest): IO[ResponseErr, AppUserId] =
    (for {
      now <- Clock.instant

      passwordHash <- passwordHashingService.hash(req.password)
      newUserId = AppUserId(UuidCreator.getTimeOrderedEpoch())
      appUser = AppUser(newUserId, req.uniqueName, req.email, passwordHash, now)

      _ <- run {
        AppUserDbTable().insertValue(lift(appUser))
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
    override protected def fromRawToParsed(req: RawRequest): IO[InvalidInputRespErr, ParsedRequest] = {
      RequestParserFor.finish {
        RequestParserFor.mapMultiple(
          "uniqueName" -> (UserUniqueName.createFrom(req.uniqueName), uniqueNameErrToContent),
          "email" -> (Email.createFrom(req.email), emailErrToContent),
          "password" -> (UserPassword.createFrom(req.password), passwordErrToContent)
        )(ParsedRequest.apply)
      }
    }

    private def uniqueNameErrToContent(err: UserUniqueNameCreationErr) = err match {
      case UserUniqueNameCreationErr.TooShort(actual, min) => (
        s"Unique name is too short. Actual length: $actual. Minimum length: $min",
        Some(s"Add at least ${min - actual} characters")
      )

      case UserUniqueNameCreationErr.TooLong(actual, max) => (
        s"Unique name is too long. Actual length: $actual. Maximum length: $max",
        Some(s"Shorten your unique name by at least ${actual - max}")
      )

      case UserUniqueNameCreationErr.InvalidChars(chars) => (
        s"Unique name contains invalid characters: ${chars.mkString(", ")}.",
        Some("Remove all invalid characters")
      )
    }

    private def emailErrToContent(err: EmailCreationErr) = err match {
      case EmailCreationErr.TooLong(actual, max) => (
        s"Email is too long. Actual length: $actual. Maximum allowed length: $max",
        Some(s"Use email address that is no longer than $max characters")
      )
      case EmailCreationErr.InvalidFormat => ("Email has invalid format", None)
    }

    private def passwordErrToContent(err: UserPasswordCreationErr) = err match {
      case UserPasswordCreationErr.TooShort(actual, min) => (
        s"Password is too short. Actual length: $actual. Minimum length: $min",
        Some(s"Add at least ${min - actual} characters")
      )

      case UserPasswordCreationErr.TooLong(actual, max) => (
        s"Password is too long. Actual length: $actual. Maximum allowed length: $max",
        Some(s"Shorten your password by at least ${actual - max} characters")
      )

      case UserPasswordCreationErr.ContainsSpaces(spacesCount) => (
        s"Password contains spaces. Spaces count: $spacesCount",
        Some("Remove all spaces from your password")
      )
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