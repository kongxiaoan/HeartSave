package com.tcm.heartsave.platform.network.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiResponseTest {

    @Test
    fun `test isSuccess with code 200`() {
        val response = ApiResponse<String>(
            code = 200,
            data = "success",
            msg = "成功"
        )
        
        assertTrue(response.isSuccess())
        assertFalse(response.isFailure())
    }

    @Test
    fun `test isFailure with code not 200`() {
        val response = ApiResponse<String>(
            code = 404,
            data = null,
            msg = "未找到"
        )
        
        assertFalse(response.isSuccess())
        assertTrue(response.isFailure())
    }

    @Test
    fun `test ApiResponse with null data`() {
        val response = ApiResponse<String>(
            code = 200,
            data = null,
            msg = "成功但无数据"
        )
        
        assertTrue(response.isSuccess())
        assertNull(response.data)
    }

    @Test
    fun `test ApiResponse with data`() {
        val response = ApiResponse(
            code = 200,
            data = mapOf("name" to "test", "age" to 18),
            msg = "成功"
        )
        
        assertTrue(response.isSuccess())
        assertNotNull(response.data)
        assertEquals("test", response.data?.get("name"))
        assertEquals(18, response.data?.get("age"))
    }
}
