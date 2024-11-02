package com.fvlaenix

import com.fvlaenix.ChannelUtils.STANDARD_IMAGE_CHANNEL_SIZE
import com.fvlaenix.alive.protobuf.isAliveRequest
import com.fvlaenix.translation.protobuf.TranslationFilesRequest
import com.fvlaenix.translation.protobuf.TranslationFilesResponse
import com.fvlaenix.translation.protobuf.TranslationRequest
import com.fvlaenix.translation.protobuf.TranslationResponse
import com.fvlaenix.translation.protobuf.TranslationServiceGrpcKt
import io.grpc.ManagedChannelBuilder

object TranslationServiceUtil {
  private suspend fun <T> withOpenedChannel(block: suspend (TranslationServiceGrpcKt.TranslationServiceCoroutineStub) -> T): T {
    val ocrChannel = ManagedChannelBuilder.forAddress(TRANSLATION_SERVICE_HOSTNAME, 50052)
      .usePlaintext()
      .maxInboundMessageSize(STANDARD_IMAGE_CHANNEL_SIZE) // 50 mb
      .build()
    val ocrChannelService = TranslationServiceGrpcKt.TranslationServiceCoroutineStub(ocrChannel)
    return ChannelUtils.runWithClose(ocrChannel, ocrChannelService, block)
  }
  
  suspend fun sendRequest(translationRequest: TranslationRequest): TranslationResponse =
    withOpenedChannel { it.translation(translationRequest) }

  suspend fun sendRequest(translationRequest: TranslationFilesRequest): TranslationFilesResponse =
    withOpenedChannel { it.translationFile(translationRequest) }

  fun isAlive(): Boolean {
    return ChannelUtils.checkServerAliveness("Translation Service") {
      withOpenedChannel { 
        it.isAlive(isAliveRequest {  })
      }
    }
  }
}