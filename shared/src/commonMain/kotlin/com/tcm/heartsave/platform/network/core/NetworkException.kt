package com.tcm.heartsave.platform.network.core

/**
 * 网络异常基类
 */
open class NetworkException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * HTTP 异常（4xx, 5xx）
 * 
 * @param statusCode HTTP 状态码
 * @param message 错误消息
 * @param body 响应体（可选）
 */
class HttpException(
    val statusCode: Int,
    message: String,
    val body: String? = null
) : NetworkException("HTTP $statusCode: $message")

/**
 * 业务异常（API 返回 code != 200）
 * 
 * @param code API 返回的业务状态码
 * @param message API 返回的错误消息
 * @param data 可选的错误数据
 */
class BusinessException(
    val code: Int,
    message: String,
    val data: Any? = null
) : NetworkException("Business Error $code: $message")

/**
 * 解析异常（JSON 解析失败等）
 * 
 * @param message 错误消息
 * @param cause 原始异常
 */
class ParseException(
    message: String,
    cause: Throwable? = null
) : NetworkException("Parse Error: $message", cause)

/**
 * 网络连接异常（连接失败、超时等）
 * 
 * @param message 错误消息
 * @param cause 原始异常
 */
class NetworkConnectionException(
    message: String = "网络连接失败",
    cause: Throwable? = null
) : NetworkException(message, cause)
