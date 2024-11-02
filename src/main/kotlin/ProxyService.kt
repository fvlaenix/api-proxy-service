package com.fvlaenix

import com.fvlaenix.alive.protobuf.IsAliveRequest
import com.fvlaenix.alive.protobuf.IsAliveResponse
import com.fvlaenix.alive.protobuf.isAliveResponse
import com.fvlaenix.database.ApiKeyConnector
import com.fvlaenix.database.ApiKeyData
import com.fvlaenix.database.DatabaseConfiguration
import com.fvlaenix.ocr.protobuf.OcrImageRequest
import com.fvlaenix.ocr.protobuf.OcrTextResponse
import com.fvlaenix.ocr.protobuf.ocrTextResponse
import com.fvlaenix.proxy.protobuf.ProxyServiceGrpcKt
import com.fvlaenix.translation.protobuf.*
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

  suspend fun checkForApiKey(): ApiKeyStatus {
    val apiKey = coroutineContext[ProxyApiInterceptor.API_KEY_ELEMENT_CONTEXT_KEY]?.apiKey
    if (apiKey == null) {
      return ApiKeyStatus.Failed("Not authorized")
    }
    if (apiKey == "TEST_API_KEY") {
      return ApiKeyStatus.Failed("Not authorized. You are using test api key. Please, set it in settings")
    }
    val apiKeyData = apiKeyConnector.get(apiKey)
    if (apiKeyData == null) {
      return ApiKeyStatus.Failed("Unknown api key")
    }
    return ApiKeyStatus.OK(apiKeyData)
  }

  override suspend fun translation(request: TranslationRequest): TranslationResponse {
    return try {
      val apiKeyStatus = checkForApiKey()
      when (apiKeyStatus) {
        is ApiKeyStatus.Failed -> translationResponse { this.error = apiKeyStatus.error }
        is ApiKeyStatus.OK -> withLogging("translation", apiKeyStatus.apiKey.name) {
          TranslationServiceUtil.sendRequest(request)
        }
      }
    } catch (e: Exception) {
      LOG.log(Level.SEVERE, "Error during prepare to translation", e)
      throw e
    }
  }

  override suspend fun translationFile(request: TranslationFilesRequest): TranslationFilesResponse {
    return try {
      val apiKeyStatus = checkForApiKey()
      when (apiKeyStatus) {
        is ApiKeyStatus.Failed -> translationFilesResponse { this.error = apiKeyStatus.error }
        is ApiKeyStatus.OK -> withLogging("translationFile", apiKeyStatus.apiKey.name) {
          TranslationServiceUtil.sendRequest(request)
        }
      }
    } catch (e: Exception) {
      LOG.log(Level.SEVERE, "Error during prepare to translation files", e)
      throw e
    }
  }

  override suspend fun ocrImage(request: OcrImageRequest): OcrTextResponse {
    return try {
      val apiKeyStatus = checkForApiKey()
      when (apiKeyStatus) {
        is ApiKeyStatus.Failed -> ocrTextResponse { this.error = apiKeyStatus.error }
        is ApiKeyStatus.OK -> withLogging("ocrImage", apiKeyStatus.apiKey.name) {
          OcrServiceUtil.sendRequest(request)
        }
      }
    } catch (e: Exception) {
      LOG.log(Level.SEVERE, "Error during prepare to ocr image", e)
      throw e
    }
  }

  override suspend fun isAlive(request: IsAliveRequest): IsAliveResponse {
    return isAliveResponse {  }
  }

  sealed class ApiKeyStatus {
    class OK(val apiKey: ApiKeyData) : ApiKeyStatus()
    class Failed(val error: String) : ApiKeyStatus()
  }
}