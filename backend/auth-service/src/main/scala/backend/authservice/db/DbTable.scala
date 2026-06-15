package backend.authservice.db

import io.getquill.{EntityQuery, Quoted, querySchema, quote}

import java.time.Instant
import java.util.UUID

trait DbTable {
  protected type TableEntity
  val name: String

  final def apply(): Quoted[EntityQuery[TableEntity]] =
    quote(querySchema[TableEntity](name))
}

