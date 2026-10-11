package com.example.client

import com.example.data.model.ChatMessage
import com.example.data.model.ChatUser
import com.example.data.model.ClientConnectionState
import com.example.data.model.RoomInfo
import com.example.server.Protocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

class ChatClient {

    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectionJob: Job? = null
    private var socket: Socket? = null
    private var writer: PrintWriter? = null

    private val _connectionState = MutableStateFlow(ClientConnectionState())
    val connectionState: StateFlow<ClientConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<ChatMessage> = _incomingMessages.asSharedFlow()

    private val _roomListUpdates = MutableStateFlow<List<RoomInfo>>(RoomInfo.defaultRooms())
    val roomListUpdates: StateFlow<List<RoomInfo>> = _roomListUpdates.asStateFlow()

    private val _roomUsers = MutableStateFlow<Map<String, List<ChatUser>>>(emptyMap())
    val roomUsers: StateFlow<Map<String, List<ChatUser>>> = _roomUsers.asStateFlow()

    private val _announcements = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val announcements: SharedFlow<String> = _announcements.asSharedFlow()

    private var currentUsername: String = ""
    private var currentRoomId: String = "general"
    private var currentAvatarIndex: Int = 0

    fun connect(
        host: String = "127.0.0.1",
        port: Int = 8888,
        username: String,
        room: String = "general",
        avatarIndex: Int = 0
    ) {
        disconnect()

        currentUsername = username
        currentRoomId = room
        currentAvatarIndex = avatarIndex

        _connectionState.value = ClientConnectionState(
            isConnected = false,
            isConnecting = true,
            serverAddress = "$host:$port",
            isLocalServer = (host == "127.0.0.1" || host == "localhost"),
            error = null
        )

        connectionJob = clientScope.launch {
            try {
                val newSocket = Socket()
                newSocket.connect(InetSocketAddress(host, port), 4000)
                socket = newSocket

                val reader = BufferedReader(InputStreamReader(newSocket.getInputStream()))
                writer = PrintWriter(newSocket.getOutputStream(), true)

                _connectionState.value = _connectionState.value.copy(
                    isConnected = true,
                    isConnecting = false,
                    error = null
                )

                // Send JOIN packet immediately
                sendPacket(Protocol.createJoinPacket(username, room, avatarIndex))

                // Listen loop
                while (isActive) {
                    val line = reader.readLine() ?: break
                    processIncomingLine(line)
                }
            } catch (e: Exception) {
                _connectionState.value = _connectionState.value.copy(
                    isConnected = false,
                    isConnecting = false,
                    error = "Gagal terhubung ke $host:$port (${e.localizedMessage ?: "Timeout"})"
                )
            } finally {
                cleanupSocket()
            }
        }
    }

    private fun processIncomingLine(line: String) {
        try {
            val json = JSONObject(line)
            when (json.optString("type")) {
                Protocol.TYPE_MSG -> {
                    val msgId = json.optString("id", UUID.randomUUID().toString())
                    val roomId = json.optString("roomId", "general")
                    val senderName = json.optString("senderName", "User")
                    val senderAvatar = json.optInt("senderAvatarIndex", 0)
                    val content = json.optString("content", "")
                    val timestamp = json.optLong("timestamp", System.currentTimeMillis())
                    val replyToSender = json.optString("replyToSender").ifEmpty { null }
                    val replyToContent = json.optString("replyToContent").ifEmpty { null }

                    val msg = ChatMessage(
                        id = msgId,
                        roomId = roomId,
                        senderName = senderName,
                        senderAvatarIndex = senderAvatar,
                        content = content,
                        timestamp = timestamp,
                        replyToSender = replyToSender,
                        replyToContent = replyToContent,
                        isMine = senderName.equals(currentUsername, ignoreCase = true)
                    )
                    _incomingMessages.tryEmit(msg)
                }

                Protocol.TYPE_SYSTEM -> {
                    val roomId = json.optString("roomId", "general")
                    val content = json.optString("content", "")
                    val timestamp = json.optLong("timestamp", System.currentTimeMillis())

                    val sysMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        roomId = roomId,
                        senderName = "Sistem",
                        content = content,
                        timestamp = timestamp,
                        isSystem = true
                    )
                    _incomingMessages.tryEmit(sysMsg)
                }

                Protocol.TYPE_ANNOUNCEMENT -> {
                    val content = json.optString("content", "")
                    _announcements.tryEmit(content)
                }

                Protocol.TYPE_ROOM_LIST -> {
                    val array = json.optJSONArray("rooms") ?: return
                    val list = mutableListOf<RoomInfo>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(
                            RoomInfo(
                                id = obj.optString("id"),
                                name = obj.optString("name"),
                                topic = obj.optString("topic"),
                                iconName = obj.optString("iconName", "chat"),
                                isPrivate = obj.optBoolean("isPrivate"),
                                password = obj.optString("password"),
                                memberCount = obj.optInt("memberCount"),
                                isDefault = obj.optBoolean("isDefault")
                            )
                        )
                    }
                    _roomListUpdates.value = list
                }

                Protocol.TYPE_USER_LIST -> {
                    val roomId = json.optString("roomId", "general")
                    val array = json.optJSONArray("users") ?: return
                    val list = mutableListOf<ChatUser>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(
                            ChatUser(
                                id = obj.optString("id"),
                                name = obj.optString("name"),
                                avatarIndex = obj.optInt("avatarIndex"),
                                status = obj.optString("status", "Online"),
                                isHost = obj.optBoolean("isHost"),
                                currentRoom = obj.optString("currentRoom")
                            )
                        )
                    }
                    val currentMap = _roomUsers.value.toMutableMap()
                    currentMap[roomId] = list
                    _roomUsers.value = currentMap
                }
            }
        } catch (_: Exception) {
            // Ignore parse exceptions for malformed packets
        }
    }

    fun sendMessage(
        content: String,
        roomId: String,
        replyToSender: String? = null,
        replyToContent: String? = null
    ) {
        val chatMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            roomId = roomId,
            senderName = currentUsername,
            senderAvatarIndex = currentAvatarIndex,
            content = content,
            timestamp = System.currentTimeMillis(),
            replyToSender = replyToSender,
            replyToContent = replyToContent,
            isMine = true
        )
        sendPacket(Protocol.createMsgPacket(chatMsg))
    }

    fun switchRoom(oldRoom: String, newRoom: String) {
        currentRoomId = newRoom
        sendPacket(Protocol.createSwitchRoomPacket(currentUsername, oldRoom, newRoom))
    }

    fun createRoom(room: RoomInfo) {
        sendPacket(Protocol.createCreateRoomPacket(room))
    }

    fun sendReaction(msgId: String, roomId: String, emoji: String) {
        sendPacket(Protocol.createReactionPacket(msgId, roomId, emoji))
    }

    private fun sendPacket(packet: String) {
        clientScope.launch {
            try {
                writer?.println(packet)
            } catch (_: Exception) {}
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        cleanupSocket()
        _connectionState.value = _connectionState.value.copy(
            isConnected = false,
            isConnecting = false
        )
    }

    private fun cleanupSocket() {
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        writer = null
    }
}
