package backend.authservice.db

import backend.authservice.domain.entities.{AppUser, UnconfirmedUser}
import backend.dbshared.DbTable
import io.getquill.*

//
//object AppUserDbTable extends DbTable[AppUser] {
//  override inline val name: "app_user" = "app_user"
//}

object AppUserDbTable {
  inline def apply(): Quoted[EntityQuery[AppUser]] =
    quote(querySchema[AppUser]("app_user"))
}

object UnconfirmedUserDbTable extends DbTable[UnconfirmedUser] {
  override inline val name: "unconfirmed_user" = "unconfirmed_user"
}




