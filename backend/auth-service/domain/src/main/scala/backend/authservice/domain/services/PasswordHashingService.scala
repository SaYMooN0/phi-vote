package backend.authservice.domain.services

import backend.authservice.domain.shared.PasswordHash
import zio.*

trait PasswordHashingService {
  def hash(password: UserPassword): Task[PasswordHash]

  def verify(passwordToVerify: UserPassword, hash: PasswordHash): Task[Boolean]
}

final case class PasswordHashingConfig(
                                        iterations: Int,
                                        memoryKiB: Int,
                                        parallelism: Int,
                                        saltLength: Int,
                                        hashLength: Int
                                      )


type UserPassword = UserPassword.Type

object UserPassword {
  opaque type Type = String

  private val MinLength = 8
  private val MaxLength = 30


  def createFrom(value: String): Either[UserPasswordCreationErr, Type] = {
    if value.length < MinLength then
      Left(UserPasswordCreationErr.TooShort(value.length, MinLength))
    else if value.length > MaxLength then
      Left(UserPasswordCreationErr.TooLong(value.length, MaxLength))
    else {
      val spacesCount = value.count(_ == ' ')

      if spacesCount > 0 then Left(UserPasswordCreationErr.ContainsSpaces(spacesCount))
      else Right(value)
    }
  }

  def unwrap(userPassword: Type): String =
    userPassword

  extension (userPassword: Type)
    def value: String = unwrap(userPassword)
}


enum UserPasswordCreationErr {
  case TooShort(actual: Int, min: Int)
  case TooLong(actual: Int, max: Int)
  case ContainsSpaces(spacesCount: Int)
}