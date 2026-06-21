package backend.authservice.domain.services

import backend.authservice.domain.shared.{Email, UserUniqueName}
import zio.Task

import java.time.Instant

trait EmailService {
  def sendRegistrationConfirmationLink(
                                        to: Email,
                                        userUniqueName: UserUniqueName,
                                        confirmationLink: String,
                                        expirationDate: Instant
                                      ): Task[Unit]

  def sendPasswordResetLink(
                             to: Email,
                             userUniqueName: UserUniqueName,
                             confirmationLink: String,
                             expirationDate: Instant
                           ): Task[Unit];

}

final case class EmailServiceConfig(
                                     sender: String,
                                     host: String,
                                     port: Int,
                                     user: String,
                                     pass: String
                                   )
