package backend.authservice.endpoints

import backend.apishared.*
import backend.apishared.resp_errs.*
import backend.authservice.*
import backend.authservice.db.UnconfirmedUserDbTable.UnconfirmedUser
import backend.authservice.db.{AppUserDbTable, UnconfirmedUserDbTable}
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*
import zio.http.*
import zio.json.ast.Json
import zio.json.{EncoderOps, JsonDecoder}

import java.sql.SQLException
import java.time.Instant
import java.util.UUID

object SignUpEndpoint extends AppEndpoint[Quill.Postgres[SnakeCase]] {

  private final case class RawRequest(uniqueName: String, email: String, password: String)derives JsonDecoder

  private final case class ParsedRequest(uniqueName: String, email: Email, password: String)


  override def handle(req: Request): ZIO[Quill.Postgres[SnakeCase], ResponseErr, Response] = {
    for {
      signUpReq <- RequestParser.parse(req.body)

      response <- ZIO.succeed {
        Response.json(
          Json.Obj(
            "isOk" -> Json.Bool(true),
            "email" -> Json.Str(signUpReq.email.value)
          ).toJson
        )
      }
    } yield response
  }


  def anyConfirmedUserWithEmail(email: Email) = {
    run {
      AppUserDbTable()
        .filter(_.email == lift(email.value))
        .take(1)
    }
  }

  private object RequestParser extends RequestParserFor[RawRequest] {
    override type Parsed = ParsedRequest

    override protected def fromRawToParsed(req: RawRequest): IO[InvalidInputRespErr, ParsedRequest] = {
      val uniqueName = req.uniqueName.trim
      val emailRaw = req.email.trim

      val emailResult =
        Email.parse(emailRaw)

      val errors = List(
        Option.when(uniqueName.length < 3) {
          InvalidInputData("uniqueName", "Unique name is too short", Some("Use at least 3 characters"))
        },
        emailResult.left.toOption,
        Option.when(req.password.length < 8) {
          InvalidInputData("password", "Password is too short", Some("Use at least 8 characters"))
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

