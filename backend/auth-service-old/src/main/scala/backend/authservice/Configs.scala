package backend.authservice

import com.typesafe.config.ConfigFactory
import zio.*

object Configs {

  def makeLayer[A: Tag](
                         path: String
                       )(using config: Config[A]): ZLayer[Any, Config.Error, A] = {
    val nestedConfig = path
      .split('.')
      .toList
      .reverse
      .foldLeft(config) { case (acc, part) =>
        acc.nested(part)
      }

    ZLayer.fromZIO(
      ZIO.config(nestedConfig)
    )
  }
}