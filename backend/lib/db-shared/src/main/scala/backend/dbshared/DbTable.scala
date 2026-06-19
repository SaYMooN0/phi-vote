package backend.dbshared

import io.getquill.*

trait DbTable[TableEntity] {
  inline val name: String;

  final inline

  def apply(): Quoted[EntityQuery[TableEntity]] =
    quote(querySchema[TableEntity](name))
}

