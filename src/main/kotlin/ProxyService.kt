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
import kotlin.coroutines.coroutineContext

class ProxyService(databaseConfiguration: DatabaseConfiguration) : ProxyServiceGrpcKt.ProxyServiceCoroutineImplBase() {
  private val apiKeyConnector = ApiKeyConnector(databaseConfiguration.toDatabase())
  
  override suspend fun translation(request: TranslationRequest): TranslationResponse {
    val apiKey = coroutineContext[ProxyApiInterceptor.API_KEY_ELEMENT_CONTEXT_KEY]?.apiKey
    if (apiKey == null) {
      return translationResponse { this.error = "Not authorized" }
    }
    if (apiKey == "TEST_API_KEY") {
      return translationResponse { this.error = "Not authorized. You are using test api key. Please, set it in settings" }
    }
    if (!apiKeyConnector.exists(apiKey)) {
      return translationResponse { this.error = "Unknown api key" }
    }
    return TranslationServiceUtil.sendRequest(request)
  }

  override suspend fun ocrImage(request: OcrImageRequest): OcrTextResponse {
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
    return OcrServiceUtil.sendRequest(request)
  }

  override suspend fun isAlive(request: IsAliveRequest): IsAliveResponse {
    return isAliveResponse {  }
  }
}