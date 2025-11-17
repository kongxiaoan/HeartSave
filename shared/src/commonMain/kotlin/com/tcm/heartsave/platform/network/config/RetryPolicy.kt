package com.tcm.heartsave.platform.network.config

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.io.IOException

/**
 * 重试策略
 * 
 * 用于定义在什么情况下重试请求
 * 
 * @param maxRetries 最大重试次数
 * @param retryDelayMillis 重试延迟（毫秒）
 * @param shouldRetry 判断是否应该重试的函数
 */
data class RetryPolicy(
    val maxRetries: Int = 3,
    val retryDelayMillis: Long = 1_000,  // 1 秒
    val shouldRetry: (Any?, Throwable) -> Boolean = { _, throwable ->
        // 默认：网络错误时重试
        throwable is IOException ||
        throwable is SocketTimeoutException ||
        throwable is ConnectTimeoutException
    }
) {
    companion object {
        /**
         * 默认重试策略（3 次，网络错误时重试）
         */
        val DEFAULT = RetryPolicy()

        /**
         * 不重试策略
         */
        val NO_RETRY = RetryPolicy(maxRetries = 0)

        /**
         * 激进重试策略（5 次，所有错误都重试）
         */
        val AGGRESSIVE = RetryPolicy(
            maxRetries = 5,
            retryDelayMillis = 500,
            shouldRetry = { _, _ -> true }
        )

        /**
         * 仅网络错误重试策略
         */
        val NETWORK_ONLY = RetryPolicy(
            maxRetries = 3,
            shouldRetry = { _, throwable ->
                throwable is IOException ||
                throwable is SocketTimeoutException ||
                throwable is ConnectTimeoutException
            }
        )
    }
}

/**
 * 执行带重试的请求
 * 
 * @param block 请求块（suspend 函数）
 * @return 请求结果
 */
public suspend inline fun <T> RetryPolicy.retry(crossinline block: suspend () -> T): T {
    var lastException: Throwable? = null
    
    repeat(maxRetries + 1) { attempt ->
        try {
            return block()
        } catch (e: Throwable) {
            lastException = e
            
            // 如果是最后一次尝试，直接抛出异常
            if (attempt == maxRetries) {
                throw e
            }
            
            // 检查是否应该重试
            // 注意：这里无法获取 HttpCall，所以使用 null
            if (!shouldRetry(null, e)) {
                throw e
            }
            
            // 延迟后重试（指数退避）
            kotlinx.coroutines.delay(retryDelayMillis * (attempt + 1))
        }
    }
    
    throw lastException ?: IllegalStateException("重试失败")
}
