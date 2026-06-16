package backend.authservice.services

import zio.{Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}


trait PasswordHashingService {
  def hash(password: String): Task[String]

  def verify(passwordToVerify: String, hash: String): Task[Boolean]
}

class PasswordHashingServiceLive() extends PasswordHashingService {

  override def hash(password: String): Task[String] = ???

  override def verify(passwordToVerify: String, hash: String): Task[Boolean] = ???
}

object PasswordHashingServiceLive {
  val layer = ZLayer.succeed(new PasswordHashingServiceLive())

  val configuredLayer = ???
  //    Configs.makeLayer[JWTConfig]("___") >>> layer
}

object PasswordHashingServiceDemo extends ZIOAppDefault {
  val program = for {
    service <- ZIO.service[PasswordHashingService]
    password <- service.hash("123")
    _ <- zio.Console.printLine(password)
  } yield ()

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    program.provide(
      PasswordHashingServiceLive.layer,
//      Configs.makeLayer[JWTConfig]("___")
    )
}
