package com.tcm.heartsave.platform.network.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class NetworkConfigTest {

    @Test
    fun `test create with baseUrl`() {
        val config = NetworkConfig.create("https://api.example.com")
        
        assertEquals("https://api.example.com", config.baseUrl)
        assertNotNull(config.timeoutConfig)
        assertNotNull(config.retryPolicy)
    }

    @Test
    fun `test create with empty baseUrl throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            NetworkConfig.create("")
        }
    }

    @Test
    fun `test create with invalid baseUrl throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            NetworkConfig.create("invalid-url")
        }
    }

    @Test
    fun `test builder pattern`() {
        val config = NetworkConfig.builder("https://api.example.com")
            .timeout(TimeoutConfig.FAST)
            .retryPolicy(RetryPolicy.NO_RETRY)
            .defaultHeader("Authorization", "Bearer token123")
            .defaultHeader("Content-Type", "application/json")
            .build()
        
        assertEquals("https://api.example.com", config.baseUrl)
        assertEquals(TimeoutConfig.FAST, config.timeoutConfig)
        assertEquals(RetryPolicy.NO_RETRY, config.retryPolicy)
        assertEquals("Bearer token123", config.defaultHeaders["Authorization"])
        assertEquals("application/json", config.defaultHeaders["Content-Type"])
    }

    @Test
    fun `test builder with defaultHeaders map`() {
        val headers = mapOf(
            "Header1" to "Value1",
            "Header2" to "Value2"
        )
        
        val config = NetworkConfig.builder("https://api.example.com")
            .defaultHeaders(headers)
            .build()
        
        assertEquals("Value1", config.defaultHeaders["Header1"])
        assertEquals("Value2", config.defaultHeaders["Header2"])
    }
}
