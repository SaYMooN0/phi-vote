package backend.auth


final case class Email private(value: String)

object Email {
  def parse(value: String): Either[InvalidInputData, Email] =
    if value.contains("@") then Right(Email(value))
    else
      Left(
        InvalidInputData(
          inputKey = "email",
          msg = "Invalid email",
          fixRec = Some("Use format name@example.com")
        )
      )
}
