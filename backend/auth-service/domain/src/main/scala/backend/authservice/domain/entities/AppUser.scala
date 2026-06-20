package backend.authservice.domain.entities

import backend.authservice.domain.shared.{Email, PasswordHash, UserUniqueName}
import backend.domainshared.AppUserId

import java.time.Instant


case class AppUser(
                    id: AppUserId,
                    uniqueName: UserUniqueName,
                    email: Email,
                    passwordHash: PasswordHash,
                    registrationDate: Instant
                  )