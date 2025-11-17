package com.tcm.heartsave.platform.network.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiResultTest {

    @Test
    fun `test Loading state`() {
        val result: ApiResult<String> = ApiResult.Loading
        
        assertTrue(result.isLoading())
        assertFalse(result.isSuccess())
        assertFalse(result.isError())
        assertNull(result.getDataOrNull())
    }

    @Test
    fun `test Success state`() {
        val data = "test data"
        val result: ApiResult<String> = ApiResult.Success(data)
        
        assertTrue(result.isSuccess())
        assertFalse(result.isLoading())
        assertFalse(result.isError())
        assertEquals(data, result.getDataOrNull())
        assertEquals(data, result.getDataOrThrow())
    }

    @Test
    fun `test HttpError state`() {
        val result: ApiResult<String> = ApiResult.Error.HttpError(
            statusCode = 404,
            message = "Not Found",
            body = "Resource not found"
        )
        
        assertTrue(result.isError())
        assertFalse(result.isSuccess())
        assertFalse(result.isLoading())
        assertNull(result.getDataOrNull())
        
        val error = result as ApiResult.Error.HttpError
        assertEquals(404, error.statusCode)
        assertEquals("Not Found", error.message)
        assertEquals("Resource not found", error.body)
    }

    @Test
    fun `test BusinessError state`() {
        val result: ApiResult<String> = ApiResult.Error.BusinessError(
            code = 1001,
            message = "业务错误",
            data = "错误详情"
        )
        
        assertTrue(result.isError())
        assertFalse(result.isSuccess())
        assertNull(result.getDataOrNull())
        
        val error = result as ApiResult.Error.BusinessError
        assertEquals(1001, error.code)
        assertEquals("业务错误", error.message)
        assertEquals("错误详情", error.data)
    }

    @Test
    fun `test NetworkError state`() {
        val cause = java.io.IOException("网络连接失败")
        val result: ApiResult<String> = ApiResult.Error.NetworkError(cause)
        
        assertTrue(result.isError())
        assertNull(result.getDataOrNull())
        
        val error = result as ApiResult.Error.NetworkError
        assertEquals(cause, error.cause)
    }

    @Test
    fun `test ParseError state`() {
        val cause = Exception("JSON 解析失败")
        val result: ApiResult<String> = ApiResult.Error.ParseError(cause)
        
        assertTrue(result.isError())
        assertNull(result.getDataOrNull())
        
        val error = result as ApiResult.Error.ParseError
        assertEquals(cause, error.cause)
    }

    @Test
    fun `test UnknownError state`() {
        val cause = Exception("未知错误")
        val result: ApiResult<String> = ApiResult.Error.UnknownError(cause)
        
        assertTrue(result.isError())
        assertNull(result.getDataOrNull())
        
        val error = result as ApiResult.Error.UnknownError
        assertEquals(cause, error.cause)
    }

    @Test
    fun `test getDataOrThrow with Success`() {
        val data = "success"
        val result: ApiResult<String> = ApiResult.Success(data)
        
        assertEquals(data, result.getDataOrThrow())
    }

    @Test
    fun `test getDataOrThrow with BusinessError throws BusinessException`() {
        val result: ApiResult<String> = ApiResult.Error.BusinessError(
            code = 1001,
            message = "业务错误"
        )
        
        try {
            result.getDataOrThrow()
            assertFalse(true, "应该抛出异常")
        } catch (e: BusinessException) {
            assertEquals(1001, e.code)
            assertEquals("业务错误", e.message)
        }
    }

    @Test
    fun `test getDataOrThrow with HttpError throws HttpException`() {
        val result: ApiResult<String> = ApiResult.Error.HttpError(
            statusCode = 500,
            message = "服务器错误"
        )
        
        try {
            result.getDataOrThrow()
            assertFalse(true, "应该抛出异常")
        } catch (e: HttpException) {
            assertEquals(500, e.statusCode)
            assertEquals("服务器错误", e.message)
        }
    }

    @Test
    fun `test getDataOrThrow with Loading throws IllegalStateException`() {
        val result: ApiResult<String> = ApiResult.Loading
        
        try {
            result.getDataOrThrow()
            assertFalse(true, "应该抛出异常")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("加载中") == true || e.message?.contains("Loading") == true)
        }
    }
}
