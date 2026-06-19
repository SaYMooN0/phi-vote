package backend.authservice.domain.shared

object Email {
  opaque type Type = String

  def unsafeFrom(value: String): Type =
    value

  def unwrap(email: Type): String =
    email

  extension (email: Type)
    def value: String = unwrap(email)
}

type Email = Email.Type