package backend.authservice.domain.shared

type Email = Email.Type

object Email {
  opaque type Type = String

  private val MaxLength = 100
  private val EmailPattern = "^[A-Za-z0-9_%+-]+(?:\\.[A-Za-z0-9_%+-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$".r

  def createFrom(value: String): Either[UserEmailCreationErr, Type] = {
    if value.length > MaxLength then
      Left(UserEmailCreationErr.TooLong(value.length, MaxLength))
    else if !EmailPattern.matches(value) then
      Left(UserEmailCreationErr.InvalidFormat)
    else
      Right(value)
  }

  def unwrap(email: Type): String =
    email

  extension (email: Type)
    def value: String = unwrap(email)
}

enum UserEmailCreationErr {
  case TooLong(actual: Int, max: Int)
  case InvalidFormat
}