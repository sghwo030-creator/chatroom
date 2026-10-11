package com.example.server

import com.example.data.model.ChatMessage
import com.example.data.model.ChatUser
import com.example.data.model.RoomInfo
import org.json.JSONArray
import org.json.JSONObject

object Protocol {
    const val TYPE_JOIN = "JOIN"
    const val TYPE_LEAVE = "LEAVE"
    const val TYPE_SWITCH_ROOM = "SWITCH_ROOM"
    const val TYPE_MSG = "MSG"
    const val TYPE_REACTION = "REACTION"
    const val TYPE_CREATE_ROOM = "CREATE_ROOM"
    const val TYPE_ROOM_LIST = "ROOM_LIST"
    const val TYPE_USER_LIST = "USER_LIST"
    const val TYPE_SYSTEM = "SYSTEM"
    const val TYPE_ANNOUNCEMENT = "ANNOUNCEMENT"
    const val TYPE_PING = "PING"
    const val TYPE_PONG = "PONG"

    fun createJoinPacket(username: String, room: String, avatarIndex: Int): String {
        return JSONObject().apply {
            put("type", TYPE_JOIN)
            put("username", username)
            put("room", room)
            put("avatarIndex", avatarIndex)
        }.toString()
    }

    fun createSwitchRoomPacket(username: String, oldRoom: String, newRoom: String): String {
        return JSONObject().apply {
            put("type", TYPE_SWITCH_ROOM)
            put("username", username)
            put("oldRoom", oldRoom)
            put("newRoom", newRoom)
        }.toString()
    }

    fun createMsgPacket(msg: ChatMessage): String {
        return JSONObject().apply {
            put("type", TYPE_MSG)
            put("id", msg.id)
            put("roomId", msg.roomId)
            put("senderName", msg.senderName)
            put("senderAvatarIndex", msg.senderAvatarIndex)
            put("content", msg.content)
            put("timestamp", msg.timestamp)
            put("replyToSender", msg.replyToSender ?: "")
            put("replyToContent", msg.replyToContent ?: "")
        }.toString()
    }

    fun createReactionPacket(msgId: String, roomId: String, emoji: String): String {
        return JSONObject().apply {
            put("type", TYPE_REACTION)
            put("msgId", msgId)
            put("roomId", roomId)
            put("emoji", emoji)
        }.toString()
    }

    fun createCreateRoomPacket(room: RoomInfo): String {
        return JSONObject().apply {
            put("type", TYPE_CREATE_ROOM)
            put("id", room.id)
            put("name", room.name)
            put("topic", room.topic)
            put("iconName", room.iconName)
            put("isPrivate", room.isPrivate)
            put("password", room.password)
        }.toString()
    }

    fun createRoomListPacket(rooms: List<RoomInfo>): String {
        val array = JSONArray()
        rooms.forEach { r ->
            array.put(JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("topic", r.topic)
                put("iconName", r.iconName)
                put("isPrivate", r.isPrivate)
                put("password", r.password)
                put("memberCount", r.memberCount)
                put("isDefault", r.isDefault)
            })
        }
        return JSONObject().apply {
            put("type", TYPE_ROOM_LIST)
            put("rooms", array)
        }.toString()
    }

    fun createUserListPacket(roomId: String, users: List<ChatUser>): String {
        val array = JSONArray()
        users.forEach { u ->
            array.put(JSONObject().apply {
                put("id", u.id)
                put("name", u.name)
                put("avatarIndex", u.avatarIndex)
                put("status", u.status)
                put("isHost", u.isHost)
                put("currentRoom", u.currentRoom)
            })
        }
        return JSONObject().apply {
            put("type", TYPE_USER_LIST)
            put("roomId", roomId)
            put("users", array)
        }.toString()
    }

    fun createSystemPacket(roomId: String, content: String): String {
        return JSONObject().apply {
            put("type", TYPE_SYSTEM)
            put("roomId", roomId)
            put("content", content)
            put("timestamp", System.currentTimeMillis())
        }.toString()
    }

    fun createAnnouncementPacket(content: String): String {
        return JSONObject().apply {
            put("type", TYPE_ANNOUNCEMENT)
            put("content", content)
            put("timestamp", System.currentTimeMillis())
        }.toString()
    }
}
