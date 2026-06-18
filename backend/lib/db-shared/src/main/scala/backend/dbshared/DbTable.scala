package backend.dbshared

import io.getquill.{EntityQuery, Quoted, querySchema, quote}

trait DbTable[TableEntity] {
  inline val name: String

  final inline def apply(): Quoted[EntityQuery[TableEntity]] =
    quote(querySchema[TableEntity](name))
}

