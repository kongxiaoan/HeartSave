package com.tcm.heartsave.platform.network.parser

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType

/**
 * JSON 解析器
 * 
 * 使用 Kotlinx Serialization 解析 JSON 响应
 */
class JsonParser(
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
) : Parser {

    override suspend fun <T : Any> parse(response: HttpResponse, typeInfo: TypeInfo<T>): T {
        try {
            val text = response.bodyAsText()
            // 使用序列化器解析
            @Suppress("UNCHECKED_CAST")
            val serializer = serializer(typeInfo.type) as kotlinx.serialization.DeserializationStrategy<T>
            return json.decodeFromString(serializer, text)
        } catch (e: SerializationException) {
            throw com.tcm.heartsave.platform.network.core.ParseException(
                "JSON 解析失败: ${e.message}",
                e
            )
        } catch (e: Exception) {
            throw com.tcm.heartsave.platform.network.core.ParseException(
                "解析响应失败: ${e.message}",
                e
            )
        }
    }

    override fun supports(contentType: String): Boolean {
        return contentType.contains("application/json", ignoreCase = true) ||
                contentType.contains("text/json", ignoreCase = true) ||
                contentType.contains("json", ignoreCase = true)
    }

    companion object {
        /**
         * 默认 JSON 解析器
         */
        val DEFAULT = JsonParser()

        /**
         * 严格模式 JSON 解析器（不忽略未知键）
         */
        val STRICT = JsonParser(
            json = Json {
                ignoreUnknownKeys = false
                isLenient = false
                coerceInputValues = false
            }
        )
    }
}

/**
 * 扩展函数：从 HttpResponse 解析 JSON
 */
suspend inline fun <reified T> HttpResponse.bodyJson(json: Json = JsonParser.DEFAULT.json): T {
    val text = bodyAsText()
    return json.decodeFromString<T>(text)
}
