package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.client.ChatClient
import com.example.data.local.ChatDatabase
import com.example.data.local.MessageEntity
import com.example.data.local.RoomEntity
import com.example.data.model.ChatMessage
import com.example.data.model.ChatServerEntry
import com.example.data.model.ChatUser
import com.example.data.model.ClientConnectionState
import com.example.data.model.RoomInfo
import com.example.data.model.ServerState
import com.example.data.repository.ServerRepository
import com.example.data.repository.UserRepository
import com.example.server.ChatServer
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val currentUsername: String = "Pengguna",
    val currentAvatarIndex: Int = 0,
    val currentStatus: String = "Sedang online",
    val userEmail: String = "",
    val currentRoomId: String = "general",
    val rooms: List<RoomInfo> = RoomInfo.defaultRooms(),
    val messages: List<ChatMessage> = emptyList(),
    val unreadCounts: Map<String, Int> = emptyMap(),
    val roomUsers: List<ChatUser> = emptyList(),
    val replyingTo: ChatMessage? = null,
    val activeAnnouncement: String? = null,
    val serverState: ServerState = ServerState(),
    val clientState: ClientConnectionState = ClientConnectionState()
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ChatDatabase.getInstance(application)
    private val dao = db.chatDao()

    private val userRepository = UserRepository(application)
    private val serverRepository = ServerRepository(application)

    val chatServer = ChatServer()
    val chatClient = ChatClient()

    private val authUser = Firebase.auth.currentUser
    private val initialName = authUser?.displayName?.ifEmpty { null }
        ?: authUser?.email?.substringBefore("@")
        ?: "Pengguna"

    private val _currentUsername = MutableStateFlow(initialName)
    private val _currentAvatarIndex = MutableStateFlow(0)
    private val _currentStatus = MutableStateFlow("Sedang online 🟢")
    private val _userEmail = MutableStateFlow(authUser?.email ?: "")
    private val _currentRoomId = MutableStateFlow("general")
    private val _rooms = MutableStateFlow<List<RoomInfo>>(RoomInfo.defaultRooms())
    private val _messagesByRoom = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    private val _unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _replyingTo = MutableStateFlow<ChatMessage?>(null)
    private val _activeAnnouncement = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ChatUiState> = combine(
        combine(_currentUsername, _currentAvatarIndex, _currentStatus, _userEmail, _currentRoomId) { u, a, s, e, r ->
            Tuple5(u, a, s, e, r)
        },
        combine(_rooms, _messagesByRoom, _unreadCounts, _replyingTo) { rm, msg, unread, reply ->
            Tuple4(rm, msg, unread, reply)
        },
        combine(chatServer.serverState, chatClient.connectionState, chatClient.roomUsers, _activeAnnouncement) { sState, cState, users, ann ->
            Tuple4(sState, cState, users, ann)
        }
    ) { (user, avatar, status, email, currentRoom), (rooms, messagesByRoom, unreads, reply), (sState, cState, roomUsersMap, announcement) ->
        val currentMessages = messagesByRoom[currentRoom] ?: emptyList()
        val currentRoomUsers = roomUsersMap[currentRoom] ?: emptyList()

        ChatUiState(
            currentUsername = user,
            currentAvatarIndex = avatar,
            currentStatus = status,
            userEmail = email,
            currentRoomId = currentRoom,
            rooms = rooms,
            messages = currentMessages,
            unreadCounts = unreads,
            roomUsers = currentRoomUsers,
            replyingTo = reply,
            activeAnnouncement = announcement,
            serverState = sState,
            clientState = cState
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatUiState())

    init {
        // Sync Firestore User Profile
        if (authUser != null) {
            viewModelScope.launch {
                userRepository.observeUserProfile()
                    .catch { /* ignore error or fallback */ }
                    .collect { profile ->
                        if (profile != null) {
                            if (profile.displayName.isNotBlank()) _currentUsername.value = profile.displayName
                            _currentAvatarIndex.value = profile.avatarIndex
                            if (profile.status.isNotBlank()) _currentStatus.value = profile.status
                        } else {
                            userRepository.saveUserProfile(
                                displayName = initialName,
                                avatarIndex = 0,
                                status = "Sedang online 🟢"
                            )
                        }
                    }
            }
        }

        // Load persistent rooms and seed defaults
        viewModelScope.launch {
            dao.insertRooms(RoomInfo.defaultRooms().map { RoomEntity.fromRoomInfo(it) })
            dao.getAllRooms().collect { entities ->
                if (entities.isNotEmpty()) {
                    _rooms.value = entities.map { it.toRoomInfo() }
                }
            }
        }

        // Listen for incoming messages from client
        viewModelScope.launch {
            chatClient.incomingMessages.collect { msg ->
                val room = msg.roomId
                val currentRoom = _currentRoomId.value
                val currentMap = _messagesByRoom.value.toMutableMap()
                val list = (currentMap[room] ?: emptyList()).toMutableList()

                // Avoid duplicates
                if (list.none { it.id == msg.id }) {
                    list.add(msg)
                    currentMap[room] = list
                    _messagesByRoom.value = currentMap

                    // Persist message to Room database
                    dao.insertMessage(MessageEntity.fromChatMessage(msg))

                    // Update unread count if received in a background room
                    if (room != currentRoom && !msg.isMine && !msg.isSystem) {
                        val unread = _unreadCounts.value.toMutableMap()
                        unread[room] = (unread[room] ?: 0) + 1
                        _unreadCounts.value = unread
                    }
                }
            }
        }

        // Listen for room updates from server
        viewModelScope.launch {
            chatClient.roomListUpdates.collect { serverRooms ->
                if (serverRooms.isNotEmpty()) {
                    _rooms.value = serverRooms
                    dao.insertRooms(serverRooms.map { RoomEntity.fromRoomInfo(it) })
                }
            }
        }

        // Listen for global announcements
        viewModelScope.launch {
            chatClient.announcements.collect { ann ->
                _activeAnnouncement.value = ann
            }
        }

        // Auto-start embedded server and connect locally on first launch
        startServerAndConnect(8888)
    }

    fun startServerAndConnect(port: Int = 8888) {
        viewModelScope.launch {
            chatServer.start(port)
            kotlinx.coroutines.delay(350)
            val name = _currentUsername.value
            chatClient.connect(
                host = "127.0.0.1",
                port = port,
                username = name,
                room = _currentRoomId.value,
                avatarIndex = _currentAvatarIndex.value
            )

            // Save server entry to Firestore for authenticated user
            if (authUser != null) {
                val serverId = "host_${port}"
                serverRepository.saveServerEntry(
                    ChatServerEntry(
                        id = serverId,
                        userId = authUser.uid,
                        name = "Server Lokal ($name)",
                        host = chatServer.serverState.value.hostIp,
                        port = port,
                        hostedLocally = true
                    )
                )
            }
        }
    }

    fun stopServer() {
        chatServer.stop()
    }

    fun connectToExternalServer(host: String, port: Int) {
        chatClient.connect(
            host = host.trim(),
            port = port,
            username = _currentUsername.value,
            room = _currentRoomId.value,
            avatarIndex = _currentAvatarIndex.value
        )
    }

    fun selectRoom(roomId: String) {
        val oldRoom = _currentRoomId.value
        if (oldRoom == roomId) return

        _currentRoomId.value = roomId

        // Clear unread count
        val unread = _unreadCounts.value.toMutableMap()
        unread.remove(roomId)
        _unreadCounts.value = unread

        // Load historical messages from Room DB for this room if not in memory
        viewModelScope.launch {
            if (_messagesByRoom.value[roomId].isNullOrEmpty()) {
                dao.getMessagesForRoom(roomId).collect { entities ->
                    val msgs = entities.map { it.toChatMessage(_currentUsername.value) }
                    val currentMap = _messagesByRoom.value.toMutableMap()
                    currentMap[roomId] = msgs
                    _messagesByRoom.value = currentMap
                }
            }
        }

        // Notify client
        chatClient.switchRoom(oldRoom, roomId)
    }

    fun sendMessage(content: String) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return

        val reply = _replyingTo.value
        chatClient.sendMessage(
            content = trimmed,
            roomId = _currentRoomId.value,
            replyToSender = reply?.senderName,
            replyToContent = reply?.content?.take(50)
        )
        _replyingTo.value = null
    }

    fun createRoom(name: String, topic: String, iconName: String, isPrivate: Boolean, password: String = "") {
        val cleanName = name.trim().lowercase().replace(" ", "-")
        val room = RoomInfo(
            id = cleanName,
            name = cleanName,
            topic = topic.trim(),
            iconName = iconName,
            isPrivate = isPrivate,
            password = password,
            isDefault = false
        )
        chatClient.createRoom(room)
        viewModelScope.launch {
            dao.insertRoom(RoomEntity.fromRoomInfo(room))
        }
        selectRoom(cleanName)
    }

    fun addReaction(messageId: String, emoji: String) {
        val room = _currentRoomId.value
        val currentMap = _messagesByRoom.value.toMutableMap()
        val list = currentMap[room]?.map { msg ->
            if (msg.id == messageId) {
                val reactions = msg.reactions.toMutableMap()
                reactions[emoji] = (reactions[emoji] ?: 0) + 1
                val updated = msg.copy(reactions = reactions)
                viewModelScope.launch {
                    dao.insertMessage(MessageEntity.fromChatMessage(updated))
                }
                updated
            } else msg
        } ?: return

        currentMap[room] = list
        _messagesByRoom.value = currentMap
        chatClient.sendReaction(messageId, room, emoji)
    }

    fun setReplyingTo(message: ChatMessage?) {
        _replyingTo.value = message
    }

    fun updateProfile(name: String, avatarIndex: Int, status: String) {
        val cleanName = name.trim().ifEmpty { "Pengguna" }
        _currentUsername.value = cleanName
        _currentAvatarIndex.value = avatarIndex
        _currentStatus.value = status.trim()

        // Sync with Firestore
        if (authUser != null) {
            viewModelScope.launch {
                userRepository.saveUserProfile(cleanName, avatarIndex, status.trim())
            }
        }

        if (chatClient.connectionState.value.isConnected) {
            // Reconnect to announce new name
            chatClient.connect(
                host = if (chatClient.connectionState.value.isLocalServer) "127.0.0.1" else chatClient.connectionState.value.serverAddress.split(":").first(),
                port = chatClient.connectionState.value.serverAddress.split(":").getOrNull(1)?.toIntOrNull() ?: 8888,
                username = cleanName,
                room = _currentRoomId.value,
                avatarIndex = avatarIndex
            )
        }
    }

    fun broadcastServerAnnouncement(announcement: String) {
        chatServer.broadcastAnnouncement(announcement.trim())
    }

    fun toggleServerBots() {
        chatServer.toggleBots()
    }

    fun dismissAnnouncement() {
        _activeAnnouncement.value = null
    }

    fun clearRoomHistory(roomId: String) {
        viewModelScope.launch {
            dao.clearMessagesForRoom(roomId)
            val currentMap = _messagesByRoom.value.toMutableMap()
            currentMap[roomId] = emptyList()
            _messagesByRoom.value = currentMap
        }
    }

    data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
    data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
}
