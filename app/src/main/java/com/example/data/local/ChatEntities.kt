package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ChatMessage
import com.example.data.model.RoomInfo

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val senderName: String,
    val senderAvatarIndex: Int,
    val content: String,
    val timestamp: Long,
    val isSystem: Boolean,
    val isAnnouncement: Boolean,
    val replyToSender: String?,
    val replyToContent: String?,
    val reactionsRaw: String = "" // e.g. "👍:2,❤️:1"
) {
    fun toChatMessage(myUserName: String): ChatMessage {
        val parsedReactions = parseReactions(reactionsRaw)
        return ChatMessage(
            id = id,
            roomId = roomId,
            senderName = senderName,
            senderAvatarIndex = senderAvatarIndex,
            content = content,
            timestamp = timestamp,
            isSystem = isSystem,
            isAnnouncement = isAnnouncement,
            replyToSender = replyToSender,
            replyToContent = replyToContent,
            isMine = (senderName.equals(myUserName, ignoreCase = true) && !isSystem && !isAnnouncement),
            reactions = parsedReactions
        )
    }

    companion object {
        fun fromChatMessage(msg: ChatMessage): MessageEntity {
            val reactionsStr = msg.reactions.entries.joinToString(",") { "${it.key}:${it.value}" }
            return MessageEntity(
                id = msg.id,
                roomId = msg.roomId,
                senderName = msg.senderName,
                senderAvatarIndex = msg.senderAvatarIndex,
                content = msg.content,
                timestamp = msg.timestamp,
                isSystem = msg.isSystem,
                isAnnouncement = msg.isAnnouncement,
                replyToSender = msg.replyToSender,
                replyToContent = msg.replyToContent,
                reactionsRaw = reactionsStr
            )
        }

        private fun parseReactions(raw: String): Map<String, Int> {
            if (raw.isBlank()) return emptyMap()
            val result = mutableMapOf<String, Int>()
            raw.split(",").forEach { pair ->
                val parts = pair.split(":")
                if (parts.size == 2) {
                    val count = parts[1].toIntOrNull() ?: 1
                    result[parts[0]] = count
                }
            }
            return result
        }
    }
}

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey val id: String,
    val name: String,
    val topic: String,
    val iconName: String,
    val isPrivate: Boolean,
    val password: String,
    val isDefault: Boolean,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toRoomInfo(): RoomInfo = RoomInfo(
        id = id,
        name = name,
        topic = topic,
        iconName = iconName,
        isPrivate = isPrivate,
        password = password,
        memberCount = 0,
        isDefault = isDefault
    )

    companion object {
        fun fromRoomInfo(room: RoomInfo): RoomEntity = RoomEntity(
            id = room.id,
            name = room.name,
            topic = room.topic,
            iconName = room.iconName,
            isPrivate = room.isPrivate,
            password = room.password,
            isDefault = room.isDefault
        )
    }
}
