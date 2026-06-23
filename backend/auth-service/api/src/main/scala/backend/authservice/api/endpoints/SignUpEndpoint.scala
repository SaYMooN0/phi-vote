package backend.authservice.api.endpoints

import backend.apishared.*
import backend.apishared.resp_errs.*
import backend.authservice.api.configs.FrontendConfig
import backend.authservice.db.{AppUserDbTable, UnconfirmedUserDbTable}
import backend.authservice.domain.entities.{AppUser, UnconfirmedUser, UnconfirmedUserConfirmationToken}
import backend.authservice.domain.services.{EmailService, PasswordHashingService, UserPassword, UserPasswordCreationErr}
import backend.authservice.domain.shared.*
import backend.dbshared.DbQuill
import backend.domainshared.UnconfirmedUserId
import com.github.f4b6a3.uuid.UuidCreator
import io.getquill.*
import zio.*
import zio.http.*
import zio.json.{JsonDecoder, JsonEncoder}

final class SignUpEndpoint private(
                                    db: DbQuill,
                                    passwordHashingService: PasswordHashingService,
                                    emailService: EmailService,
                                    frontendConfig: FrontendConfig
                                  ) extends AppEndpoint {

  import backend.authservice.db.AppUserTable.given
  import backend.authservice.db.UnconfirmedUserTableMappings.given
  import db.*

  private final case class RawRequest(uniqueName: String, email: String, password: String)derives JsonDecoder

  private final case class ParsedRequest(uniqueName: UserUniqueName, email: Email, password: UserPassword)

  private final case class ResponseResult(email: String)derives JsonEncoder

  override def handle(httpReq: Request): IO[ResponseErr, Response] =
    for {
      req <- RequestParser.parse(httpReq.body)
      unconfirmedUser <- saveUnconfirmedUserInDb(req)
      confirmationLinkExpirationDate <- Clock.instant.map(_.plusSeconds(15 * 60))

      _ <- emailService
        .sendRegistrationConfirmationLink(
          unconfirmedUser.email,
          unconfirmedUser.uniqueName,
          registrationConfirmationLinkFor(unconfirmedUser),
          confirmationLinkExpirationDate
        )
        .mapError(_ => InternalServerErrorRespErr(Some("Could not send registration confirmation email")))
    } yield OkResponse(
      ResponseResult(req.email.value)
    )

  private def saveUnconfirmedUserInDb(req: ParsedRequest): IO[ResponseErr, UnconfirmedUser] =
    for {
      confirmedUserExists <- run {
        AppUserDbTable()
          .filter(_.email == lift(req.email))
          .take(1)
      }
        .map(_.nonEmpty)
        .mapError(_ => InternalServerErrorRespErr.DbOpFailed)

      _ <- if confirmedUserExists
      then ZIO.fail(JustMsgRespErr(s"Account with email '${req.email.value}' already exists", Status.Conflict))
      else ZIO.unit

      passwordHash <- passwordHashingService
        .hash(req.password)
        .mapError(_ => InternalServerErrorRespErr(Some("Password hashing failed")))

      unconfirmedUser <- transaction {
        for {
          existingOpt <- run {
            UnconfirmedUserDbTable()
              .filter(_.email == lift(req.email))
              .take(1)
          }.map(_.headOption)

          unconfirmedUser <- saveUnconfirmedUser(existingOpt, req.uniqueName, req.email, passwordHash)
        } yield unconfirmedUser
      }
        .mapError(_ => InternalServerErrorRespErr.DbOpFailed)
    } yield unconfirmedUser

  private def saveUnconfirmedUser(
                                   existingUnconfirmedUser: Option[UnconfirmedUser],
                                   uniqueName: UserUniqueName,
                                   email: Email,
                                   passwordHash: PasswordHash
                                 ): Task[UnconfirmedUser] = {
    val newConfirmationToken = UnconfirmedUserConfirmationToken(UuidCreator.getTimeOrderedEpoch())
    existingUnconfirmedUser match {
      case Some(existingUser) => {
        val updatedUser = existingUser.copy(
          uniqueName = uniqueName,
          passwordHash = passwordHash,
          confirmationToken = newConfirmationToken
        )

        run {
          UnconfirmedUserDbTable()
            .filter(_.id == lift(existingUser.id))
            .update(
              _.uniqueName -> lift(updatedUser.uniqueName),
              _.passwordHash -> lift(updatedUser.passwordHash),
              _.confirmationToken -> lift(updatedUser.confirmationToken)
            )
        }.as(updatedUser)
      }
      case None => {
        val newUser = UnconfirmedUser(
          id = UnconfirmedUserId(UuidCreator.getTimeOrderedEpoch()),
          uniqueName, email, passwordHash, newConfirmationToken
        )

        run {
          UnconfirmedUserDbTable()
            .insertValue(lift(newUser))
        }.as(newUser)
      }
    }
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
      case UserUniqueNameCreationErr.TooShort(actual, min) => {
        (s"Unique name is too short: $actual characters provided, but at least $min required. Add at least ${min - actual} characters", None)
      }
      case UserUniqueNameCreationErr.TooLong(actual, max) => {
        (s"Unique name is too long: $actual characters provided, but no more than $max allowed. Shorten it by at least ${actual - max} characters", None)
      }
      case UserUniqueNameCreationErr.InvalidChars(chars) => {
        (s"Unique name contains invalid characters: ${chars.mkString(", ")}. Remove them and use only English letters, hyphen and underscore", None)
      }
    }

    private def emailErrToContent(err: EmailCreationErr) = err match {
      case EmailCreationErr.TooLong(actual, max) => {
        (s"Email is too long: $actual characters provided, but no more than $max allowed. Use a shorter email address", None)
      }
      case EmailCreationErr.InvalidFormat => {
        ("Email has invalid format. Use an address like user@example.com", None)
      }
    }

    private def passwordErrToContent(err: UserPasswordCreationErr) = err match {
      case UserPasswordCreationErr.TooShort(actual, min) => {
        (s"Password is too short: $actual characters provided, but at least $min required. Add at least ${min - actual} characters", None)
      }
      case UserPasswordCreationErr.TooLong(actual, max) => {
        (s"Password is too long: $actual characters provided, but no more than $max allowed. Shorten it by at least ${actual - max} characters", None)
      }
      case UserPasswordCreationErr.ContainsSpaces(spacesCount) => {
        (s"Password contains $spacesCount space characters. Remove all spaces from your password", None)
      }
    }
  }
}

object SignUpEndpoint extends EndpointProviderFor[SignUpEndpoint] {

  val live: ZLayer[
    DbQuill & PasswordHashingService & EmailService & FrontendConfig,
    Nothing,
    SignUpEndpoint
  ] =
    ZLayer.fromFunction {
      (
        quill: DbQuill,
        passwordHashingService: PasswordHashingService,
        emailService: EmailService,
        frontendConfig: FrontendConfig
      ) =>
        new SignUpEndpoint(quill, passwordHashingService, emailService, frontendConfig)
    }
}