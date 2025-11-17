package com.tcm.heartsave.platform.network.factory

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js

/**
 * WasmJs 平台的 HttpClient 工厂实现
 * 
 * 注意：WasmJs 使用 JS Engine
 */
actual object HttpClientFactory {
    actual fun create(block: HttpClientConfig<*>.() -> Unit): HttpClient {
        return HttpClient(Js, block)
    }

    actual fun createEngine(): HttpClientEngine {
        return Js.create()
    }
}
