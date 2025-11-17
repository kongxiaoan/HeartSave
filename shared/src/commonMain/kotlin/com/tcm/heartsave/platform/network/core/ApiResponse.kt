package com.tcm.heartsave.platform.network.core

import kotlinx.serialization.Serializable

/**
 * 标准 API 响应格式
 * 
 * 大多数后端 API 使用这种格式：
 * {
 *   "code": 200,
 *   "data": {...},
 *   "msg": "success"
 * }
 * 
 * @param code 业务状态码（200 表示成功）
 * @param data 业务数据（泛型，可为 null）
 * @param msg 消息（通常用于错误消息）
 */
@Serializable
data class ApiResponse<T>(
    val code: Int,
    val data: T? = null,
    val msg: String = ""
) {
    /**
     * 检查业务是否成功
     */
    fun isSuccess(): Boolean = code == 200

    /**
     * 检查业务是否失败
     */
    fun isFailure(): Boolean = !isSuccess()
}
