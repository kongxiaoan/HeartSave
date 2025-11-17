package com.tcm.heartsave.platform.network.parser

import io.ktor.client.statement.HttpResponse

/**
 * 解析器接口
 * 
 * 用于将 HTTP 响应解析为指定类型
 */
interface Parser {
    /**
     * 解析响应为指定类型
     * 
     * @param response HTTP 响应
     * @param typeInfo 类型信息（用于泛型类型擦除场景）
     * @return 解析后的对象
     */
    suspend fun <T : Any> parse(response: HttpResponse, typeInfo: TypeInfo<T>): T

    /**
     * 检查是否支持该内容类型
     * 
     * @param contentType 内容类型（如 "application/json"）
     * @return 是否支持
     */
    fun supports(contentType: String): Boolean
}

/**
 * 类型信息
 * 
 * 用于在运行时保存泛型类型信息
 */
data class TypeInfo<T : Any>(
    val type: kotlin.reflect.KType,
    val reifiedType: kotlin.reflect.KClass<T>
)
