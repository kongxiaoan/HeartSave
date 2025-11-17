package com.tcm.heartsave.platform.network.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest

class RetryPolicyTest {

    @Test
    fun `test default retry policy`() {
        val policy = RetryPolicy.DEFAULT
        
        assertEquals(3, policy.maxRetries)
        assertEquals(1_000L, policy.retryDelayMillis)
    }

    @Test
    fun `test no retry policy`() {
        val policy = RetryPolicy.NO_RETRY
        
        assertEquals(0, policy.maxRetries)
    }

    @Test
    fun `test aggressive retry policy`() {
        val policy = RetryPolicy.AGGRESSIVE
        
        assertEquals(5, policy.maxRetries)
        assertEquals(500L, policy.retryDelayMillis)
    }

    @Test
    fun `test retry with success on first attempt`() = runTest {
        var attemptCount = 0
        val policy = RetryPolicy.DEFAULT
        
        val result = policy.retry {
            attemptCount++
            "success"
        }
        
        assertEquals("success", result)
        assertEquals(1, attemptCount)
    }

    @Test
    fun `test retry with success on second attempt`() = runTest {
        var attemptCount = 0
        val policy = RetryPolicy.DEFAULT
        
        val result = policy.retry {
            attemptCount++
            if (attemptCount < 2) {
                throw java.io.IOException("网络错误")
            }
            "success"
        }
        
        assertEquals("success", result)
        assertEquals(2, attemptCount)
    }

    @Test
    fun `test retry exhausts all attempts`() = runTest {
        var attemptCount = 0
        val policy = RetryPolicy(maxRetries = 2)
        
        try {
            policy.retry {
                attemptCount++
                throw java.io.IOException("网络错误")
            }
            assertFalse(true, "应该抛出异常")
        } catch (e: java.io.IOException) {
            assertEquals(3, attemptCount) // maxRetries + 1
        }
    }

    @Test
    fun `test retry with non-retryable exception`() = runTest {
        var attemptCount = 0
        val policy = RetryPolicy.DEFAULT
        
        try {
            policy.retry {
                attemptCount++
                throw IllegalArgumentException("不应该重试的错误")
            }
            assertFalse(true, "应该抛出异常")
        } catch (e: IllegalArgumentException) {
            assertEquals(1, attemptCount) // 不应该重试
        }
    }

    @Test
    fun `test shouldRetry with IOException returns true`() {
        val policy = RetryPolicy.DEFAULT
        val exception = java.io.IOException("网络错误")
        
        assertTrue(policy.shouldRetry(null, exception))
    }

    @Test
    fun `test shouldRetry with SocketTimeoutException returns true`() {
        val policy = RetryPolicy.DEFAULT
        val exception = java.net.SocketTimeoutException("超时")
        
        assertTrue(policy.shouldRetry(null, exception))
    }

    @Test
    fun `test shouldRetry with IllegalArgumentException returns false`() {
        val policy = RetryPolicy.DEFAULT
        val exception = IllegalArgumentException("参数错误")
        
        assertFalse(policy.shouldRetry(null, exception))
    }
}
