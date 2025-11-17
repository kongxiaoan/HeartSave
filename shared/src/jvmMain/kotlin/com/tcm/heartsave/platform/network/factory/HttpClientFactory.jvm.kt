package com.tcm.heartsave.platform.network.factory

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

/**
 * JVM 平台的 HttpClient 工厂实现
 */
actual object HttpClientFactory {
    actual fun create(block: HttpClientConfig<*>.() -> Unit): HttpClient {
        return HttpClient(CIO, block)
    }

    actual fun createEngine(): HttpClientEngine {
        return CIO.create()
    }
}
