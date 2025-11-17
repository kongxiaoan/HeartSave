package com.tcm.heartsave

import com.tcm.heartsave.platform.network.core.NetworkClient

class Greeting {
    private val platform = getPlatform()

    private val client: NetworkClient by lazy {
        NetworkClient.create("https://ktor.io")
    }
    
    suspend fun greet(): String {
        val res = client.get<String>("/docs/")
        return "${res.data}"
    }
}