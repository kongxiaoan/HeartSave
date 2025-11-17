package com.tcm.heartsave.platform.network.core

/**
 * 统一的 API 结果封装
 * 
 * 用于表示网络请求的各种状态：
 * - Loading: 请求进行中
 * - Success: 请求成功，包含业务数据
 * - Error: 请求失败，包含错误信息
 */
sealed class ApiResult<out T> {
    /**
     * 请求进行中
     */
    object Loading : ApiResult<Nothing>()

    /**
     * 请求成功
     * 
     * @param data 业务数据
     */
    data class Success<T>(val data: T) : ApiResult<T>()

    /**
     * 请求失败
     */
    sealed class Error : ApiResult<Nothing>() {
        /**
         * HTTP 错误（4xx, 5xx）
         * 
         * @param statusCode HTTP 状态码
         * @param message 错误消息
         * @param body 响应体（可选）
         */
        data class HttpError(
            val statusCode: Int,
            val message: String,
            val body: String? = null
        ) : Error()

        /**
         * 业务错误（API 返回 code != 200）
         * 
         * @param code API 返回的业务状态码
         * @param message API 返回的错误消息
         * @param data 可选的错误数据
         */
        data class BusinessError(
            val code: Int,
            val message: String,
            val data: Any? = null
        ) : Error()

        /**
         * 网络错误（连接失败、超时等）
         * 
         * @param cause 原始异常
         */
        data class NetworkError(val cause: Throwable) : Error()

        /**
         * 解析错误（JSON 解析失败等）
         * 
         * @param cause 原始异常
         */
        data class ParseError(val cause: Throwable) : Error()

        /**
         * 未知错误
         * 
         * @param cause 原始异常
         */
        data class UnknownError(val cause: Throwable) : Error()
    }
}

/**
 * 检查是否为成功状态
 */
fun <T> ApiResult<T>.isSuccess(): Boolean = this is ApiResult.Success

/**
 * 检查是否为错误状态
 */
fun <T> ApiResult<T>.isError(): Boolean = this is ApiResult.Error

/**
 * 检查是否为加载状态
 */
fun <T> ApiResult<T>.isLoading(): Boolean = this is ApiResult.Loading

/**
 * 获取数据，如果失败则返回 null
 */
fun <T> ApiResult<T>.getDataOrNull(): T? = when (this) {
    is ApiResult.Success -> data
    else -> null
}

/**
 * 获取数据，如果失败则抛出异常
 */
fun <T> ApiResult<T>.getDataOrThrow(): T = when (this) {
    is ApiResult.Success -> data
    is ApiResult.Error.BusinessError -> throw BusinessException(code, message, data)
    is ApiResult.Error.HttpError -> throw HttpException(statusCode, message, body)
    is ApiResult.Error.NetworkError -> throw cause
    is ApiResult.Error.ParseError -> throw cause
    is ApiResult.Error.UnknownError -> throw cause
    is ApiResult.Loading -> throw IllegalStateException("数据还在加载中")
}
