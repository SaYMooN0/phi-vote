package backend.authservice.api.services

import backend.apishared.Configs
import backend.authservice.api.configs.EmailServiceConfig
import backend.authservice.domain.services.EmailService
import backend.authservice.domain.shared.{Email, UserUniqueName}
import zio.*
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.TypesafeConfigProvider
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Properties
import javax.mail.*
import javax.mail.internet.MimeMessage

private final class EmailServiceLive private(config: EmailServiceConfig) extends EmailService {
  private val sender: String = config.sender
  private val host: String = config.host
  private val port: Int = config.port
  private val user: String = config.user
  private val pass: String = config.pass


  override def sendRegistrationConfirmationLink(
                                                 to: Email,
                                                 userUniqueName: UserUniqueName,
                                                 confirmationLink: String,
                                                 expirationDate: Instant
                                               ): Task[Unit] = {
    sendEmail(
      to = to,
      subject = "Confirm your registration",
      content =
        s"""Hello ${userUniqueName.value},
           |
           |Please confirm your registration by opening this link:
           |
           |$confirmationLink
           |
           |This link expires at:
           |${DateTimeFormatter.ISO_INSTANT.format(expirationDate)}
           |
           |If you did not create an account, you can ignore this email.
           |""".stripMargin
    )
  }

  override def sendPasswordResetConfirmationLink(
                                                  to: Email,
                                                  userUniqueName: UserUniqueName,
                                                  confirmationLink: String,
                                                  passwordFirstChar: Char,
                                                  passwordLastChar: Char,
                                                  expirationDate: Instant
                                                ): Task[Unit] = {
    sendEmail(
      to = to,
      subject = "Confirm password reset",
      content =
        s"""Hello ${userUniqueName.value},
           |
           |We received a request to reset your password.
           |
           |New password will start with '$passwordFirstChar' and end with '$passwordLastChar'.
           |
           |To confirm password reset, open this link:
           |
           |$confirmationLink
           |
           |This link expires at:
           |${DateTimeFormatter.ISO_INSTANT.format(expirationDate)}
           |
           |If you did not request password reset, you can ignore this email.
           |""".stripMargin
    )
  }

  private def sendEmail(to: Email, subject: String, content: String): Task[Unit] = {
    val messageZIO = for {
      prop <- propsResource
      session <- createSession(prop)
      message <- createMessage(session)(sender, to.value, subject, content)
    } yield message

    messageZIO.map(message => Transport.send(message))
  }

  private def propsResource: Task[Properties] =
    ZIO.attempt {
      val props = Properties()
      props.put("mail.smtp.host", config.host)
      props.put("mail.smtp.port", config.port.toString)

      props.put("mail.smtp.auth", "true")

      // for 587 port
      props.put("mail.smtp.starttls.enable", "true")
      props.put("mail.smtp.starttls.required", "true")

      props.put("mail.smtp.connectiontimeout", "10000")
      props.put("mail.smtp.timeout", "10000")
      props.put("mail.smtp.writetimeout", "10000")

      //      props.put("mail.debug", "true")
      props
    }

  private def createSession(prop: Properties): Task[Session] = ZIO.attempt {
    Session.getInstance(
      prop,
      new Authenticator {
        override protected def getPasswordAuthentication = new PasswordAuthentication(user, pass)
      }
    )
  }

  private def createMessage(
                             session: Session
                           )(from: String, to: String, subject: String, content: String): Task[MimeMessage] = {
    val message = new MimeMessage(session)
    message.setFrom(from)
    message.setRecipients(Message.RecipientType.TO, to)
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")
    ZIO.succeed(message)
  }
}

object EmailServiceLive {

  import EmailServiceConfig.given

  val layer = ZLayer {
    ZIO.service[EmailServiceConfig].map(config => new EmailServiceLive(config))
  }

  val configuredLayer = Configs.makeLayer[EmailServiceConfig]("emailService") >>> layer
}

object EmailServiceDemo extends ZIOAppDefault {
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.setConfigProvider(TypesafeConfigProvider.fromResourcePath())

  private def parseForDemo[E, A](result: Either[E, A]): Task[A] =
    ZIO.fromEither(result).mapError(err => RuntimeException(err.toString))

  private val program = for {
    emailService <- ZIO.service[EmailService]

    to <- parseForDemo(Email.createFrom("___"))
    userUniqueName <- parseForDemo(UserUniqueName.createFrom("demo_user"))
    expirationDate = Instant.now().plusSeconds(15.minutes.toSeconds)
    _ <- Console.printLine("Starting...")
    _ <- emailService.sendRegistrationConfirmationLink(to, userUniqueName, "reg link", expirationDate)
    _ <- Console.printLine("Registration confirmation email done.")
    _ <- emailService.sendPasswordResetConfirmationLink(to, userUniqueName, "reset link", 'q', '9', expirationDate)
    _ <- Console.printLine("Password reset confirmation email done.")
  } yield ()

  override def run = program.provide(EmailServiceLive.configuredLayer)
}