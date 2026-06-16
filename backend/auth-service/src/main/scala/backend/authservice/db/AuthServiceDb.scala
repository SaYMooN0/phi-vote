package backend.authservice.db

import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill

type AuthServiceDb = Quill.Postgres[SnakeCase]