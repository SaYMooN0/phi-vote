package backend.authservice.api.endpoints

import backend.authservice.db.*
import backend.authservice.domain.*
import io.getquill.*
import zio.*
import zio.http.*
import zio.json.*
import backend.apishared.*
import backend.apishared.resp_errs.*
import backend.authservice.api.configs.FrontendConfig
import backend.authservice.db.{AppUserDbTable, UnconfirmedUserDbTable}
import backend.authservice.domain.entities.{AppUser, UnconfirmedUser, UnconfirmedUserConfirmationToken}
import backend.authservice.domain.services.{EmailService, PasswordHashingService, UserPassword, UserPasswordCreationErr}
import backend.authservice.domain.shared.*
import backend.dbshared.DbQuill
import backend.domainshared.{AppUserId, UnconfirmedUserId}
import backend.authservice.db.{AppUserDbTable, UnconfirmedUserDbTable}

import java.util.UUID

final class ConfirmRegistrationEndpoint private(db: DbQuill) extends AppEndpoint {

  import backend.authservice.db.AppUserTable.given
  import backend.authservice.db.UnconfirmedUserTableMappings.given
  import db.*

  private final case class RawRequest(userId: String, confirmationCode: String) derives JsonDecoder

  private final case class ParsedRequest(userId: UnconfirmedUserId, confirmationCode: UUID)

  private final case class ResponseResult(userUniqueName: String, email: String) derives JsonEncoder

  private final case class UserAlreadyConfirmedResponseErr(email: String) extends ResponseErr

  private case object ConfirmationLinkIsInvalidResponseErr extends ResponseErr

  private case object UserToConfirmNotFound extends ResponseErr

  override def handle(httpReq: Request): IO[ResponseErr, Response] = {
    for {
      req <- RequestParser.parse(httpReq.body)

      confirmed <- confirmUser(req.userId, req.confirmationCode).catchSome {
        case UserToConfirmNotFound =>
          findAlreadyConfirmedUser(req.userId).flatMap {
            case Some(user) => ZIO.fail(UserAlreadyConfirmedResponseErr(user.email.value))
            case None => ZIO.fail(ConfirmationLinkIsInvalidResponseErr)
          }
      }
    } yield OkResponse(
      ResponseResult(
        userUniqueName = confirmed.uniqueName.value,
        email = confirmed.email.value
      )
    )
  }

  private def confirmUser(userId: UnconfirmedUserId, confirmationCode: UUID): IO[UserToConfirmNotFound.type, AppUser] = {
    transaction {
      for {
        unconfirmedOpt <- run {
          quote {
            UnconfirmedUserDbTable()
              .filter(user => user.id == lift(userId) && user.confirmationCode == lift(confirmationCode))
          }
        }.map(_.headOption)

        unconfirmed <- ZIO
          .fromOption(unconfirmedOpt)
          .orElseFail(UserToConfirmNotFound)

        confirmed = AppUser(
          id = AppUserId(unconfirmed.id.value),
          uniqueName = unconfirmed.uniqueName,
          email = unconfirmed.email,
          passwordHash = unconfirmed.passwordHash,
          registrationDate = now
        )

        _ <- run {
          quote {
            AppUserDbTable().insertValue(lift(confirmed))
          }
        }

        _ <- run {
          quote {
            UnconfirmedUserDbTable()
              .filter(user => user.id == lift(userId))
              .delete
          }
        }
      } yield confirmed
    }
  }

  private def findAlreadyConfirmedUser(
    userId: UnconfirmedUserId
  ): IO[Nothing, Option[AppUser]] = {
    val appUserId = AppUserId.fromUnconfirmedUserId(userId)

    run {
      quote {
        AppUserDbTable()
          .filter(user => user.id == lift(appUserId))
      }
    }.map(_.headOption).orDie
  }

  private object RequestParser extends RequestParserFor[ParsedRequest, RawRequest] {

    override protected def fromRawToParsed(
      req: RawRequest
    ): IO[InvalidInputRespErr, ParsedRequest] = {
      val parsed =
        for {
          userIdUuid <- parseUuid(req.userId)
          confirmationCode <- parseUuid(req.confirmationCode)
        } yield ParsedRequest(
          userId = UnconfirmedUserId.fromUuid(userIdUuid),
          confirmationCode = confirmationCode
        )

      ZIO.fromEither(parsed).mapError { _ =>
        InvalidInputRespErr.one("other", "Confirmation link is invalid")
      }
    }
  }
}

object ConfirmRegistrationEndpoint
  extends EndpointProviderFor[ConfirmRegistrationEndpoint] {

  val live: ZLayer[
    DbQuill,
    Nothing,
    ConfirmRegistrationEndpoint
  ] =
    ZLayer.fromFunction { (quill: DbQuill) =>
      new ConfirmRegistrationEndpoint(quill)
    }
}