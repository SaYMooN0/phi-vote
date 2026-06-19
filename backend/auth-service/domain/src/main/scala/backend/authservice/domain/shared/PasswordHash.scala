package backend.authservice.domain.shared

object PasswordHash {
  opaque type Type = String

  def unsafeFrom(value: String): Type = value

  extension (hash: Type)
    def value: String = hash
}

type PasswordHash = PasswordHash.Type
