package com.tcm.heartsave.platform.network.factory

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine

/**
 * HttpClient 工厂
 * 
 * 用于创建平台特定的 HttpClient 实例
 * 使用 expect/actual 机制实现跨平台支持
 */
expect object HttpClientFactory {
    /**
     * 创建 HttpClient 实例
     * 
     * @param block 配置块，用于自定义 HttpClient 配置
     * @return HttpClient 实例
     */
    fun create(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient

    /**
     * 创建 HttpClientEngine
     * 
     * 用于需要自定义 Engine 的场景
     * 
     * @return HttpClientEngine 实例
     */
    fun createEngine(): HttpClientEngine
}
