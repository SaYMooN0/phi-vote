package backend.authservice.domain.entities

import backend.authservice.domain.shared.{Email, PasswordHash, UserUniqueName}
import backend.domainshared.{UnconfirmedUserId, UuidWrapperCompanion}

final case class UnconfirmedUser(
                                  id: UnconfirmedUserId,
                                  uniqueName: UserUniqueName,
                                  email: Email,
                                  passwordHash: PasswordHash,
                                  confirmationToken: UnconfirmedUserConfirmationToken
                                )


type UnconfirmedUserConfirmationToken = UnconfirmedUserConfirmationToken.Type

object UnconfirmedUserConfirmationToken extends UuidWrapperCompanion
