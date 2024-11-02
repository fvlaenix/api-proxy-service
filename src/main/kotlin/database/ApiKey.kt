package com.fvlaenix.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

data class ApiKeyData(
  val name: String,
  val apiKey: String
)

object ApiKeyTable : Table() {
  val name = varchar("name", 127).primaryKey()
  val apiKey = varchar("api_key", 127)
  
  init {
    index(true, apiKey)
  }
}

class ApiKeyConnector(val database: Database) {
  init {
    transaction(database) {
      SchemaUtils.create(ApiKeyTable)
    }
  }
  
  fun exists(apiKey: String): Boolean = transaction(database) {
    ApiKeyTable.select { ApiKeyTable.apiKey eq apiKey }.count() > 0
  }

  fun getName(apiKey: String): String? = transaction(database) {
    ApiKeyTable.select { ApiKeyTable.apiKey eq apiKey }.map { it[ApiKeyTable.name] }.firstOrNull()
  }
}