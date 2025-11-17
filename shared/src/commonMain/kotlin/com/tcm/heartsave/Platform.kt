package com.tcm.heartsave

interface Platform {
    val name: String
}

/**
 * expect = 声明
 * actual = 实现
 */
expect fun getPlatform(): Platform