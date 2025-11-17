package com.tcm.heartsave.platform.network.config

/**
 * 网络配置
 * 
 * 用于配置 NetworkClient 的各种参数
 * 
 * @param baseUrl 基础 URL
 * @param timeoutConfig 超时配置
 * @param retryPolicy 重试策略（可选）
 * @param defaultHeaders 默认请求头
 */
data class NetworkConfig(
    val baseUrl: String,
    val timeoutConfig: TimeoutConfig = TimeoutConfig.DEFAULT,
    val retryPolicy: RetryPolicy? = RetryPolicy.DEFAULT,
    val defaultHeaders: Map<String, String> = emptyMap()
) {
    init {
        require(baseUrl.isNotBlank()) { "baseUrl 不能为空" }
        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            "baseUrl 必须以 http:// 或 https:// 开头"
        }
    }

    companion object {
        /**
         * 创建配置的 Builder
         */
        fun builder(baseUrl: String) = Builder(baseUrl)

        /**
         * 快速创建配置
         */
        fun create(baseUrl: String): NetworkConfig {
            return NetworkConfig(baseUrl = baseUrl)
        }
    }

    /**
     * 配置 Builder
     */
    class Builder(private val baseUrl: String) {
        private var timeoutConfig: TimeoutConfig = TimeoutConfig.DEFAULT
        private var retryPolicy: RetryPolicy? = RetryPolicy.DEFAULT
        private val defaultHeaders = mutableMapOf<String, String>()

        fun timeout(timeoutConfig: TimeoutConfig) = apply {
            this.timeoutConfig = timeoutConfig
        }

        fun retryPolicy(retryPolicy: RetryPolicy?) = apply {
            this.retryPolicy = retryPolicy
        }

        fun defaultHeader(name: String, value: String) = apply {
            defaultHeaders[name] = value
        }

        fun defaultHeaders(headers: Map<String, String>) = apply {
            defaultHeaders.putAll(headers)
        }

        fun build(): NetworkConfig {
            return NetworkConfig(
                baseUrl = baseUrl,
                timeoutConfig = timeoutConfig,
                retryPolicy = retryPolicy,
                defaultHeaders = defaultHeaders
            )
        }
    }
}
