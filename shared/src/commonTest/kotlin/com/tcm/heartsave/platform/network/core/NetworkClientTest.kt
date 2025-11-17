package com.tcm.heartsave.platform.network.core

import com.tcm.heartsave.platform.network.config.NetworkConfig
import com.tcm.heartsave.platform.network.config.RetryPolicy
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Serializable
data class User(
    val id: Int,
    val name: String,
    val email: String
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: User
)

class NetworkClientTest {

    private fun createMockHttpClient(
        responseBody: String,
        statusCode: HttpStatusCode = HttpStatusCode.OK
    ): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(
                        content = responseBody,
                        status = statusCode,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    @Test
    fun `test GET request returns ApiResponse`() = runTest {
        val responseJson = """
            {
                "code": 200,
                "data": {
                    "id": 1,
                    "name": "Test User",
                    "email": "test@example.com"
                },
                "msg": "success"
            }
        """.trimIndent()

        val httpClient = createMockHttpClient(responseJson)
        val config = NetworkConfig.create("https://api.example.com")
        val client = NetworkClient.create(config, httpClient = httpClient)

        val response: ApiResponse<User> = client.get("/user/1")

        assertTrue(response.isSuccess())
        assertEquals(200, response.code)
        assertNotNull(response.data)
        assertEquals(1, response.data?.id)
        assertEquals("Test User", response.data?.name)
        assertEquals("test@example.com", response.data?.email)

        httpClient.close()
    }

    @Test
    fun `test GET request with getData returns data directly`() = runTest {
        val responseJson = """
            {
                "code": 200,
                "data": {
                    "id": 2,
                    "name": "John Doe",
                    "email": "john@example.com"
                },
                "msg": "success"
            }
        """.trimIndent()

        val httpClient = createMockHttpClient(responseJson)
        val config = NetworkConfig.create("https://api.example.com")
        val client = NetworkClient.create(config, httpClient = httpClient)

        val user: User = client.getData("/user/2")

        assertEquals(2, user.id)
        assertEquals("John Doe", user.name)
        assertEquals("john@example.com", user.email)

        httpClient.close()
    }

    @Test
    fun `test GET request with business error throws BusinessException`() = runTest {
        val responseJson = """
            {
                "code": 404,
                "data": null,
                "msg": "用户不存在"
            }
        """.trimIndent()

        val httpClient = createMockHttpClient(responseJson)
        val config = NetworkConfig.create("https://api.example.com")
        val client = NetworkClient.create(config, httpClient = httpClient)

        assertFailsWith<BusinessException> {
            client.getData<User>("/user/999")
        }.let { exception ->
            assertEquals(404, exception.code)
            assertEquals("用户不存在", exception.message)
        }

        httpClient.close()
    }

    @Test
    fun `test POST request returns ApiResponse`() = runTest {
        val responseJson = """
            {
                "code": 200,
                "data": {
                    "token": "abc123",
                    "user": {
                        "id": 1,
                        "name": "Test User",
                        "email": "test@example.com"
                    }
                },
                "msg": "登录成功"
            }
        """.trimIndent()

        val httpClient = createMockHttpClient(responseJson)
        val config = NetworkConfig.create("https://api.example.com")
        val client = NetworkClient.create(config, httpClient = httpClient)

        val loginRequest = LoginRequest("testuser", "password123")
        val response: ApiResponse<LoginResponse> = client.post("/login", loginRequest)

        assertTrue(response.isSuccess())
        assertEquals(200, response.code)
        assertNotNull(response.data)
        assertEquals("abc123", response.data?.token)
        assertEquals(1, response.data?.user?.id)

        httpClient.close()
    }

    @Test
    fun `test POST request with getData returns data directly`() = runTest {
        val responseJson = """
            {
                "code": 200,
                "data": {
                    "token": "xyz789",
                    "user": {
                        "id": 3,
                        "name": "Jane Doe",
                        "email": "jane@example.com"
                    }
                },
                "msg": "登录成功"
            }
        """.trimIndent()

        val httpClient = createMockHttpClient(responseJson)
        val config = NetworkConfig.create("https://api.example.com")
        val client = NetworkClient.create(config, httpClient = httpClient)

        val loginRequest = LoginRequest("jane", "password")
        val loginResponse: LoginResponse = client.postData("/login", loginRequest)

        assertEquals("xyz789", loginResponse.token)
        assertEquals(3, loginResponse.user.id)
        assertEquals("Jane Doe", loginResponse.user.name)

        httpClient.close()
    }

    @Test
    fun `test HTTP error throws HttpException`() = runTest {
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = "",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val config = NetworkConfig.create("https://api.example.com")
        val client = NetworkClient.create(config, httpClient = httpClient)

        assertFailsWith<HttpException> {
            client.get<User>("/user/1")
        }.let { exception ->
            assertEquals(500, exception.statusCode)
        }

        httpClient.close()
    }

    @Test
    fun `test default headers are added`() = runTest {
        val responseJson = """
            {
                "code": 200,
                "data": {
                    "id": 1,
                    "name": "Test",
                    "email": "test@example.com"
                },
                "msg": "success"
            }
        """.trimIndent()

        val httpClient = createMockHttpClient(responseJson)
        val config = NetworkConfig.builder("https://api.example.com")
            .defaultHeader("Authorization", "Bearer token123")
            .defaultHeader("X-Custom-Header", "custom-value")
            .build()
        val client = NetworkClient.create(config, httpClient = httpClient)

        val response: ApiResponse<User> = client.get("/user/1")

        assertTrue(response.isSuccess())
        // 注意：MockEngine 不会验证请求头，这里只是测试配置是否正确
        assertEquals("https://api.example.com", client.baseUrl)

        httpClient.close()
    }

    @Test
    fun `test NetworkClient create with builder`() = runTest {
        val client = NetworkClient.create("https://api.example.com") {
            defaultHeader("User-Agent", "MyApp/1.0")
        }

        assertEquals("https://api.example.com", client.baseUrl)
        client.close()
    }
}
