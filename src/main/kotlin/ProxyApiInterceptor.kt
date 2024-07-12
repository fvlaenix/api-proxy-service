package com.fvlaenix

import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.kotlin.CoroutineContextServerInterceptor
import kotlin.coroutines.CoroutineContext

class ProxyApiInterceptor : CoroutineContextServerInterceptor() {
  companion object {
    val AUTHORIZATION_KEY: Metadata.Key<String> = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER)

    class ApiKeyElement(val apiKey: String?) : CoroutineContext.Element {
      override val key = API_KEY_ELEMENT_CONTEXT_KEY
    }
    val API_KEY_ELEMENT_CONTEXT_KEY = object : CoroutineContext.Key<ApiKeyElement> {}
  }
  
  override fun coroutineContext(call: ServerCall<*, *>, headers: Metadata): CoroutineContext {
    val apiKey = headers.get(AUTHORIZATION_KEY)
    return ApiKeyElement(apiKey)
  }
}