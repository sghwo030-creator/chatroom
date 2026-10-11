package com.example.data.model

data class ChatMessage(
    val id: String,
    val roomId: String,
    val senderName: String,
    val senderAvatarIndex: Int = 0,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSystem: Boolean = false,
    val isAnnouncement: Boolean = false,
    val replyToSender: String? = null,
    val replyToContent: String? = null,
    val isMine: Boolean = false,
    val reactions: Map<String, Int> = emptyMap()
)
