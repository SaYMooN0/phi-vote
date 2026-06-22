package backend.authservice.api.endpoints

import backend.apishared.*
import backend.apishared.resp_errs.*
import backend.authservice.db.AppUserDbTable
import backend.authservice.domain.entities.{AppUser, UnconfirmedUser}
import backend.authservice.domain.services.{EmailService, PasswordHashingService, UserPassword, UserPasswordCreationErr}
import backend.authservice.domain.shared.{Email, EmailCreationErr, UserUniqueName, UserUniqueNameCreationErr}
import backend.dbshared.DbQuill
import backend.domainshared.{AppUserId, UnconfirmedUserId}
import com.github.f4b6a3.uuid.UuidCreator
import io.getquill.*
import zio.*
import zio.http.*
import zio.json.{JsonDecoder, JsonEncoder}

final class SignUpEndpoint private(
                                    db: DbQuill,
                                    passwordHashingService: PasswordHashingService,
                                    emailService: EmailService
                                  ) extends AppEndpoint {

  import backend.authservice.db.AppUserTableMappings.given
  import backend.authservice.db.UnconfirmedUserTableMappings.given
  import db.*

  private final case class RawRequest(uniqueName: String, email: String, password: String)derives JsonDecoder

  private final case class ParsedRequest(uniqueName: UserUniqueName, email: Email, password: UserPassword)

  private final case class ResponseResult(email: String)derives JsonEncoder

  override def handle(httpReq: Request): IO[ResponseErr, Response] =
    for {
      req <- RequestParser.parse(httpReq.body)

      unconfirmedUser <- register(req)

      confirmationLinkExpirationDate <- Clock.instant.map(_.plusSeconds(15 * 60))

      _ <- emailService
        .sendRegistrationConfirmationLink(
          to = req.email,
          userUniqueName = req.uniqueName,
          confirmationLink = registrationConfirmationLinkFor(unconfirmedUser),
          expirationDate = confirmationLinkExpirationDate
        )
        .mapError(_ =>
          InternalServerRespErr("Could not send registration confirmation email")
        )
    } yield OkResponse(
      ResponseResult(req.email.value)
    )

  private def register(req: ParsedRequest): IO[ResponseErr, UnconfirmedUser] =
    transaction {
      for {
        confirmedUserExists <- run {
          AppUserDbTable()
            .filter(_.email == lift(req.email))
            .take(1)
        }
          .map(_.nonEmpty)
          .mapError(_ => InternalServerRespErr("Database operation failed"))

        _ <-
          if confirmedUserExists then
            ZIO.fail(
              ConflictRespErr(
                msg = "This email is already taken",
                details = s"Profile with email '${req.email.value}' already exists"
              )
            )
          else
            ZIO.unit

        existingUnconfirmedUser <- run {
          UnconfirmedUserDbTable()
            .filter(_.email == lift(req.email))
            .take(1)
        }
          .map(_.headOption)
          .mapError(_ => InternalServerRespErr("Database operation failed"))

        passwordHash <- passwordHashingService
          .hash(req.password)
          .mapError(_ => InternalServerRespErr("Could not hash password"))

        unconfirmedUser <- existingUnconfirmedUser match {
          case Some(existingUser) =>
            val updatedUser = existingUser.copy(
              uniqueName = req.uniqueName,
              passwordHash = passwordHash
              // если у тебя есть confirmationToken / updatedAt / expirationDate,
              // обновляй их здесь тоже
            )

            run {
              UnconfirmedUserDbTable()
                .filter(_.id == lift(existingUser.id))
                .updateValue(lift(updatedUser))
            }
              .as(updatedUser)
              .mapError(_ => InternalServerRespErr("Database operation failed"))

          case None =>
            val newUser = UnconfirmedUser(
              id = UnconfirmedUserId(UuidCreator.getTimeOrderedEpoch()),
              uniqueName = req.uniqueName,
              email = req.email,
              passwordHash = passwordHash
            )

            run {
              UnconfirmedUserDbTable()
                .insertValue(lift(newUser))
            }
              .as(newUser)
              .mapError(_ => InternalServerRespErr("Database operation failed"))
        }
      } yield unconfirmedUser
    }
  private def registrationConfirmationLinkFor(unconfirmedUser: UnconfirmedUser): String =
    s"${frontendConfig.baseUrl}" +
      s"/${frontendConfig.confirmRegistrationPath}" +
      s"?id=${unconfirmedUser.id.value}" +
      s"&t=${unconfirmedUser.confirmationToken.value}"

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
    DbQuill & PasswordHashingService & EmailService,
    Nothing,
    SignUpEndpoint
  ] =
    ZLayer.fromFunction {
      (
        quill: DbQuill,
        passwordHashingService: PasswordHashingService,
        emailService: EmailService
      ) =>
        new SignUpEndpoint(quill, passwordHashingService, emailService)
    }
}