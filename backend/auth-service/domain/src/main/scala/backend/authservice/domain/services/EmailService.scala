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

  def sendPasswordResetConfirmationLink(
                             to: Email,
                             userUniqueName: UserUniqueName,
                             confirmationLink: String,
                             passwordFirstChar: Char,
                             passwordLastChar: Char,
                             expirationDate: Instant
                           ): Task[Unit];

}

