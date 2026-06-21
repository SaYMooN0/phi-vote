package backend.authservice.api.services

import backend.authservice.domain.services.{EmailService, EmailServiceConfig}
import zio.*
import java.util.Properties
import javax.mail.Session
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.internet.MimeMessage
import javax.mail.Transport
import javax.mail.Message

final class EmailServiceLive private(config: EmailServiceConfig) extends EmailService {
  private val host: String = config.host
  private val port: Int = config.port
  private val user: String = config.user
  private val pass: String = config.pass

  override def sendEmail(to: String, subject: String, content: String): Task[Unit] = {
    val messageZIO = for {
      prop <- propsResource
      session <- createSession(prop)
      message <- createMessage(session)("daniel@rockthejvm.com", to, subject, content)
    } yield message

    messageZIO.map(message => Transport.send(message))
  }

  private val propsResource: Task[Properties] = {
    val prop = new Properties
    prop.put("mail.smtp.auth", true)
    prop.put("mail.smtp.starttls.enable", "true")
    prop.put("mail.smtp.host", host)
    prop.put("mail.smtp.port", port)
    prop.put("mail.smtp.ssl.trust", host)
    ZIO.succeed(prop)
  }

  private def createSession(prop: Properties): Task[Session] = ZIO.attempt {
    Session.getInstance(
      prop,
      new Authenticator {
        override protected def getPasswordAuthentication(): PasswordAuthentication =
          new PasswordAuthentication(user, pass)
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

object EmailServiceConfig {
  given Config[EmailServiceConfig] = deriveConfig[EmailServiceConfig]
}

object EmailServiceLive {

  import EmailServiceConfig.given

  val layer = ZLayer {
    ZIO.service[EmailServiceConfig].map(config => new EmailServiceLive(config))
  }

  val configuredLayer =
    Configs.makeLayer[EmailServiceConfig]("rockthejvm.email") >>> layer
}

object EmailServiceDemo extends ZIOAppDefault {
  val program = for {
    emailService <- ZIO.service[EmailService]
    _ <- emailService.sendPasswordRecoveryEmail("spiderman@rockthejvm.com", "ABCD1234")
    _ <- Console.printLine("Email done.")
  } yield ()

  override def run = program.provide(EmailServiceLive.configuredLayer)
}
