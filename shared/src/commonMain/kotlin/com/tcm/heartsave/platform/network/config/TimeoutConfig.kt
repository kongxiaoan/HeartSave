package com.tcm.heartsave.platform.network.config

/**
 * 超时配置
 * 
 * 用于配置 HTTP 请求的各种超时时间
 * 
 * @param connectTimeoutMillis 连接超时（毫秒）
 * @param socketTimeoutMillis Socket 超时（毫秒）
 * @param requestTimeoutMillis 请求超时（毫秒）
 */
data class TimeoutConfig(
    val connectTimeoutMillis: Long = 30_000,      // 30 秒
    val socketTimeoutMillis: Long = 30_000,      // 30 秒
    val requestTimeoutMillis: Long = 60_000      // 60 秒
) {
    companion object {
        /**
         * 默认超时配置
         */
        val DEFAULT = TimeoutConfig()

        /**
         * 快速超时配置（用于快速失败场景）
         */
        val FAST = TimeoutConfig(
            connectTimeoutMillis = 5_000,
            socketTimeoutMillis = 5_000,
            requestTimeoutMillis = 10_000
        )

        /**
         * 慢速超时配置（用于大文件上传/下载）
         */
        val SLOW = TimeoutConfig(
            connectTimeoutMillis = 60_000,
            socketTimeoutMillis = 60_000,
            requestTimeoutMillis = 300_000  // 5 分钟
        )
    }
}
