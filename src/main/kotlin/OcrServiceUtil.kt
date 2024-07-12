package com.fvlaenix

import com.fvlaenix.ChannelUtils.STANDARD_IMAGE_CHANNEL_SIZE
import com.fvlaenix.alive.protobuf.isAliveRequest
import com.fvlaenix.ocr.protobuf.OcrImageRequest
import com.fvlaenix.ocr.protobuf.OcrServiceGrpcKt
import com.fvlaenix.ocr.protobuf.OcrTextResponse
import io.grpc.ManagedChannelBuilder

object OcrServiceUtil {
  private suspend fun <T> withOpenedChannel(block: suspend (OcrServiceGrpcKt.OcrServiceCoroutineStub) -> T): T {
    val ocrChannel = ManagedChannelBuilder.forAddress(OCR_SERVICE_HOSTNAME, 50051)
      .usePlaintext()
      .maxInboundMessageSize(STANDARD_IMAGE_CHANNEL_SIZE) // 50 mb
      .build()
    val ocrChannelService = OcrServiceGrpcKt.OcrServiceCoroutineStub(ocrChannel)
    return ChannelUtils.runWithClose(ocrChannel, ocrChannelService, block)
  }
  
  suspend fun sendRequest(ocrTextResponse: OcrImageRequest): OcrTextResponse =
    withOpenedChannel { it.ocrImage(ocrTextResponse) }

  fun isAlive(): Boolean {
    return ChannelUtils.checkServerAliveness("Translation Service") {
      withOpenedChannel {
        it.isAlive(isAliveRequest { })
      }
    }
  }
}