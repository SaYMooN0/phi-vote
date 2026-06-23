package backend.domainshared


import com.github.f4b6a3.uuid.UuidCreator

import java.util.UUID

trait UuidWrapperCompanion {
  opaque type Type = UUID

  def apply(value: UUID): Type = value

  def unwrap(id: Type): UUID = id

  extension (id: Type) {
    def value: UUID = unwrap(id)
  }

  def createNew(): Type = UuidCreator.getTimeOrderedEpoch()
}