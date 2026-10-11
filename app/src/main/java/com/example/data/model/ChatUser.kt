package com.example.data.model

data class ChatUser(
    val id: String,
    val name: String,
    val avatarIndex: Int = 0,
    val status: String = "Online",
    val isHost: Boolean = false,
    val currentRoom: String = "general"
)

data class ServerState(
    val isRunning: Boolean = false,
    val hostIp: String = "127.0.0.1",
    val port: Int = 8888,
    val connectedClientsCount: Int = 0,
    val totalMessagesRelayed: Int = 0,
    val uptimeSeconds: Long = 0,
    val botsEnabled: Boolean = true,
    val logs: List<String> = emptyList()
)

data class ClientConnectionState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val serverAddress: String = "127.0.0.1:8888",
    val isLocalServer: Boolean = true,
    val error: String? = null
)
