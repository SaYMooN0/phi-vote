package backend.authservice.domain.entities

import backend.authservice.domain.shared.{Email, PasswordHash}
import backend.domainshared.AppUserId

import java.time.Instant


case class AppUser(
                    id: AppUserId, // UUID
                    uniqueName: String, //<64
                    email: Email, // <128
                    passwordHash: PasswordHash,
                    createdAt: Instant,
                    registrationDate: Instant
                  )