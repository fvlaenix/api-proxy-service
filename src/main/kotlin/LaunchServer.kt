package com.fvlaenix

import com.fvlaenix.database.DatabaseConfiguration
import java.io.InputStream
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger
import kotlin.io.path.Path
import kotlin.io.path.inputStream

const val LOGGING_PATH: String = "/logging.properties"

val DATABASE_PROPERTIES_PATH_STRING: String? = System.getenv("DATABASE_PROPERTIES")

val DATABASE_PROPERTIES_INPUT_STREAM: InputStream =
  DATABASE_PROPERTIES_PATH_STRING?.let { try { Path(it).inputStream() } catch (e: Exception) { throw Exception("Can't open file", e) } } ?:
  LaunchServer::class.java.getResourceAsStream("database.properties") ?:
  throw IllegalStateException("Cannot find database properties in standard files")

val OCR_SERVICE_HOSTNAME: String = System.getenv("OCR_SERVICE_HOSTNAME") ?: "localhost"
val TRANSLATION_SERVICE_HOSTNAME: String = System.getenv("TRANSLATION_SERVICE_HOSTNAME") ?: "localhost"

class LaunchServer

fun main() {
  try {
    LogManager.getLogManager().readConfiguration(LaunchServer::class.java.getResourceAsStream(LOGGING_PATH))
  } catch (e: Exception) {
    throw IllegalStateException("Failed while trying to read logs", e)
  }
  val log = Logger.getLogger(LaunchServer::class.java.name)
  log.log(Level.INFO, "Trying to get database configuration")
  val databaseConfiguration = DatabaseConfiguration(DATABASE_PROPERTIES_INPUT_STREAM)
  log.log(Level.INFO, "Test database connection")
  databaseConfiguration.toDatabase()
  log.log(Level.INFO, "Bot and database configurations loaded successfully")
  log.log(Level.INFO, "Check proxy aliveness")
  if (!TranslationServiceUtil.isAlive()) return
  if (!OcrServiceUtil.isAlive()) return
  log.log(Level.INFO, "Starting gRPC server")
  val server = ProxyServer(50058, databaseConfiguration)
  server.start()
  log.log(Level.INFO, "Launched")
  server.blockUntilShutdown()
}