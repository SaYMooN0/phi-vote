package backend.authservice.domain.shared

type UserUniqueName = UserUniqueName.Type

object UserUniqueName {
  opaque type Type = String

  private val MinLength = 4
  private val MaxLength = 60

  private def isAllowedChar(ch: Char): Boolean =
    (ch >= 'a' && ch <= 'z') ||
      (ch >= 'A' && ch <= 'Z') ||
      ch == '-' ||
      ch == '_'

  def createFrom(value: String): Either[UserUniqueNameCreationErr, Type] = {
    if value.length < MinLength then
      Left(UserUniqueNameCreationErr.TooShort(value.length, MinLength))
    else if value.length > MaxLength then
      Left(UserUniqueNameCreationErr.TooLong(value.length, MaxLength))
    else {
      val invalidChars = value.filterNot(isAllowedChar).distinct.toList

      if invalidChars.nonEmpty then
        Left(UserUniqueNameCreationErr.InvalidChars(invalidChars))
      else
        Right(value)
    }
  }

  def unwrap(userUniqueName: Type): String =
    userUniqueName

  extension (userUniqueName: Type)
    def value: String = unwrap(userUniqueName)
}


enum UserUniqueNameCreationErr {
  case TooShort(actual: Int, min: Int)
  case TooLong(actual: Int, max: Int)
  case InvalidChars(chars: List[Char])
}