package com.tcm.heartsave.platform.network.core

import com.tcm.heartsave.platform.network.config.NetworkConfig
import com.tcm.heartsave.platform.network.config.retry
import com.tcm.heartsave.platform.network.factory.HttpClientFactory
import com.tcm.heartsave.platform.network.parser.JsonParser
import com.tcm.heartsave.platform.network.parser.Parser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

/**
 * 网络客户端
 * 
 * 统一的网络请求入口，提供简洁的 API
 * 
 * 使用示例：
 * ```
 * val client = NetworkClient.create("https://api.example.com")
 * val user: User = client.getData("/user/123")
 * ```
 */
class NetworkClient private constructor(
    @PublishedApi internal val httpClient: HttpClient,
    @PublishedApi internal val config: NetworkConfig,
    internal val parser: Parser = JsonParser.DEFAULT
) {
    val baseUrl: String get() = config.baseUrl

    /**
     * GET 请求（返回标准 API 响应）
     * 
     * @param path 请求路径（相对于 baseUrl）
     * @param headers 额外的请求头
     * @return ApiResponse<T> 标准 API 响应
     */
    suspend inline fun <reified T> get(
        path: String,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse<T> {
        return executeRequest {
            httpClient.get(baseUrl + path) {
                // 添加默认请求头
                config.defaultHeaders.forEach { (key, value) ->
                    header(key, value)
                }
                // 添加额外请求头
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
        }
    }

    /**
     * GET 请求（直接返回数据，自动处理业务错误）
     * 
     * @param path 请求路径
     * @param headers 额外的请求头
     * @return T 业务数据
     * @throws BusinessException 如果 code != 200
     */
    suspend inline fun <reified T> getData(
        path: String,
        headers: Map<String, String> = emptyMap()
    ): T {
        val response = get<T>(path, headers)
        
        // 检查业务状态码
        if (response.code != 200) {
            throw BusinessException(response.code, response.msg, response.data)
        }
        
        // 检查数据是否为空
        return response.data ?: throw ParseException("响应数据为空")
    }

    /**
     * POST 请求（返回标准 API 响应）
     * 
     * @param path 请求路径
     * @param body 请求体（可序列化对象）
     * @param headers 额外的请求头
     * @return ApiResponse<T> 标准 API 响应
     */
    suspend inline fun <reified T, reified B : Any> post(
        path: String,
        body: B? = null,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse<T> {
        return executeRequest {
            httpClient.post(baseUrl + path) {
                // 添加默认请求头
                config.defaultHeaders.forEach { (key, value) ->
                    header(key, value)
                }
                // 添加额外请求头
                headers.forEach { (key, value) ->
                    header(key, value)
                }
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
        }
    }

    /**
     * POST 请求（直接返回数据，自动处理业务错误）
     * 
     * @param path 请求路径
     * @param body 请求体
     * @param headers 额外的请求头
     * @return T 业务数据
     * @throws BusinessException 如果 code != 200
     */
    suspend inline fun <reified T, reified B : Any> postData(
        path: String,
        body: B? = null,
        headers: Map<String, String> = emptyMap()
    ): T {
        val response = post<T, B>(path, body, headers)
        
        // 检查业务状态码
        if (response.code != 200) {
            throw BusinessException(response.code, response.msg, response.data)
        }
        
        // 检查数据是否为空
        return response.data ?: throw ParseException("响应数据为空")
    }

    /**
     * 执行请求（内部方法）
     */
    public suspend inline fun <reified T> executeRequest(
        crossinline request: suspend () -> HttpResponse
    ): ApiResponse<T> {
        return withContext(Dispatchers.Default) {
            try {
                // 执行请求（带重试）
                val response = config.retryPolicy?.retry {
                    request()
                } ?: request()

                // 检查 HTTP 状态码
                val status = response.status
                if (status.value < 200 || status.value >= 300) {
                    val bodyText = try {
                        response.bodyAsText()
                    } catch (e: Exception) {
                        null
                    }
                    throw HttpException(
                        statusCode = status.value,
                        message = status.description,
                        body = bodyText
                    )
                }

                // 解析响应
                response.body<ApiResponse<T>>()
            } catch (e: HttpException) {
                throw e
            } catch (e: BusinessException) {
                throw e
            } catch (e: ParseException) {
                throw e
            } catch (e: SocketTimeoutException) {
                throw NetworkConnectionException("请求超时", e)
            } catch (e: ConnectTimeoutException) {
                throw NetworkConnectionException("连接失败", e)
            } catch (e: IOException) {
                throw NetworkConnectionException("网络错误", e)
            } catch (e: Throwable) {
                throw NetworkException("未知错误: ${e.message}", e)
            }
        }
    }

    /**
     * 关闭客户端
     */
    fun close() {
        httpClient.close()
    }

    companion object {
        /**
         * 创建 NetworkClient
         * 
         * @param config 网络配置
         * @param parser 解析器（默认 JSON）
         * @param httpClient 可选的 HttpClient（用于测试）
         * @return NetworkClient 实例
         */
        fun create(
            config: NetworkConfig,
            parser: Parser = JsonParser.DEFAULT,
            httpClient: HttpClient? = null
        ): NetworkClient {
            val client = httpClient ?: HttpClientFactory.create {
                // 配置超时（注意：不同平台的超时配置方式可能不同）
                // 这里使用通用的配置方式
                expectSuccess = false  // 不自动抛出异常，手动处理
            }
            
            return NetworkClient(client, config, parser)
        }

        /**
         * 快速创建 NetworkClient（使用默认配置）
         * 
         * @param baseUrl 基础 URL
         * @return NetworkClient 实例
         */
        fun create(baseUrl: String): NetworkClient {
            return create(NetworkConfig.create(baseUrl))
        }

        /**
         * 使用 Builder 创建 NetworkClient
         * 
         * @param baseUrl 基础 URL
         * @param block Builder 配置块
         * @return NetworkClient 实例
         */
        inline fun create(
            baseUrl: String,
            block: NetworkConfig.Builder.() -> Unit
        ): NetworkClient {
            val config = NetworkConfig.builder(baseUrl).apply(block).build()
            return create(config)
        }
    }
}

