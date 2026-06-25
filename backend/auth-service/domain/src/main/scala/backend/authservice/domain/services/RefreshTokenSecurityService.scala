package backend.authservice.domain.services

import backend.authservice.domain.entities.{RefreshTokenHash, RefreshTokenPlain}
import zio.*


trait RefreshTokenSecurityService {
  def generate: Task[RefreshTokenPlain]

  def hash(token: RefreshTokenPlain): Task[RefreshTokenHash]
}
