package com.fvlaenix

import com.fvlaenix.alive.protobuf.IsAliveRequest
import com.fvlaenix.alive.protobuf.IsAliveResponse
import com.fvlaenix.alive.protobuf.isAliveResponse
import com.fvlaenix.database.ApiKeyConnector
import com.fvlaenix.database.DatabaseConfiguration
import com.fvlaenix.ocr.protobuf.OcrImageRequest
import com.fvlaenix.ocr.protobuf.OcrTextResponse
import com.fvlaenix.ocr.protobuf.ocrTextResponse
import com.fvlaenix.proxy.protobuf.ProxyServiceGrpcKt
import com.fvlaenix.translation.protobuf.TranslationRequest
import com.fvlaenix.translation.protobuf.TranslationResponse
import com.fvlaenix.translation.protobuf.translationResponse
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.coroutineContext

private val LOG: Logger = Logger.getLogger(ProxyService::class.java.name)

class ProxyService(databaseConfiguration: DatabaseConfiguration) : ProxyServiceGrpcKt.ProxyServiceCoroutineImplBase() {
  private val apiKeyConnector = ApiKeyConnector(databaseConfiguration.toDatabase())

  suspend fun <T> withLogging(taskName: String, name: String?, task: suspend () -> T): T {
    LOG.info("Starting $taskName by user with name $name")
    return try {
      task()
    } finally {
      LOG.info("Finished $taskName by user with name $name")
    }
  }

  override suspend fun translation(request: TranslationRequest): TranslationResponse {
    return try {
      val apiKey = coroutineContext[ProxyApiInterceptor.API_KEY_ELEMENT_CONTEXT_KEY]?.apiKey
      if (apiKey == null) {
        return translationResponse { this.error = "Not authorized" }
      }
      if (apiKey == "TEST_API_KEY") {
        return translationResponse {
          this.error = "Not authorized. You are using test api key. Please, set it in settings"
        }
      }
      if (!apiKeyConnector.exists(apiKey)) {
        return translationResponse { this.error = "Unknown api key" }
      }
      withLogging("translation", apiKeyConnector.getName(apiKey)) {
        TranslationServiceUtil.sendRequest(request)
      }
    } catch (e: Exception) {
      LOG.log(Level.SEVERE, "Error during prepare to translation", e)
      throw e
    }
  }

  override suspend fun ocrImage(request: OcrImageRequest): OcrTextResponse {
    return try {
      val apiKey = coroutineContext[ProxyApiInterceptor.API_KEY_ELEMENT_CONTEXT_KEY]?.apiKey
      if (apiKey == null) {
        return ocrTextResponse { this.error = "Not authorized" }
      }
      if (apiKey == "TEST_API_KEY") {
        return ocrTextResponse { this.error = "Not authorized. You are using test api key. Please, set it in settings" }
      }
      if (!apiKeyConnector.exists(apiKey)) {
        return ocrTextResponse { this.error = "Unknown api key" }
      }
      withLogging("ocrImage", apiKeyConnector.getName(apiKey)) {
        OcrServiceUtil.sendRequest(request)
      }
    } catch (e: Exception) {
      LOG.log(Level.SEVERE, "Error during prepare to ocr image", e)
      throw e
    }
  }

  override suspend fun isAlive(request: IsAliveRequest): IsAliveResponse {
    return isAliveResponse {  }
  }
}