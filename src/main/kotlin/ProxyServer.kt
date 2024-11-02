package com.fvlaenix

import com.fvlaenix.ChannelUtils.STANDARD_IMAGE_CHANNEL_SIZE
import com.fvlaenix.database.DatabaseConfiguration
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptors

class ProxyServer(port: Int, databaseConfiguration: DatabaseConfiguration) {
  private val server: Server = ServerBuilder.forPort(port)
    .addService(ServerInterceptors.intercept(ProxyService(databaseConfiguration), ProxyApiInterceptor()))
    .maxInboundMessageSize(STANDARD_IMAGE_CHANNEL_SIZE)
    .build()

  fun start() {
    server.start()
    Runtime.getRuntime().addShutdownHook(
      Thread {
        this@ProxyServer.stop()
      }
    )
  }

  private fun stop() {
    server.shutdown()
  }

  fun blockUntilShutdown() {
    server.awaitTermination()
  }
}