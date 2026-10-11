package com.example.server

import com.example.data.model.ChatMessage
import com.example.data.model.ChatUser
import com.example.data.model.RoomInfo
import com.example.data.model.ServerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class ChatServer {

    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private var timerJob: Job? = null
    private var botJob: Job? = null

    private val clientSessions = ConcurrentHashMap<String, ClientSession>()
    private val rooms = CopyOnWriteArrayList<RoomInfo>()

    private val _serverState = MutableStateFlow(ServerState())
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    data class ClientSession(
        val id: String = UUID.randomUUID().toString(),
        val socket: Socket,
        val reader: BufferedReader,
        val writer: PrintWriter,
        var username: String = "",
        var currentRoom: String = "general",
        var avatarIndex: Int = 0,
        var isHost: Boolean = false
    )

    init {
        // Initialize default rooms
        rooms.addAll(RoomInfo.defaultRooms())
    }

    fun start(port: Int = 8888) {
        if (_serverState.value.isRunning) return

        val localIp = NetworkUtils.getLocalIpAddress()
        log("[SERVER] Memulai server ChatRoom pada port $port...")

        serverJob = serverScope.launch {
            try {
                serverSocket = ServerSocket(port).apply {
                    reuseAddress = true
                }

                _serverState.value = _serverState.value.copy(
                    isRunning = true,
                    hostIp = localIp,
                    port = port,
                    uptimeSeconds = 0,
                    connectedClientsCount = 0
                )

                log("[SERVER] 🟢 Server aktif di $localIp:$port. Menunggu koneksi client...")

                // Start uptime counter
                startUptimeTracker()

                // Start simulation companion bots
                startBotCompanion()

                while (isActive) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        handleNewClient(clientSocket)
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                log("[SERVER] ❌ Gagal membuka server: ${e.localizedMessage}")
                stop()
            }
        }
    }

    private fun handleNewClient(socket: Socket) {
        serverScope.launch {
            val sessionId = UUID.randomUUID().toString()
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)
                val session = ClientSession(id = sessionId, socket = socket, reader = reader, writer = writer)

                clientSessions[sessionId] = session
                updateClientCount()

                val clientIp = socket.inetAddress?.hostAddress ?: "Unknown"
                log("[CONNECT] Client terhubung dari $clientIp (Sesi: ${sessionId.take(6)})")

                // Send initial room list to new client
                sendToSession(session, Protocol.createRoomListPacket(getRoomsWithCounts()))

                // Listen for messages from this client
                while (isActive) {
                    val line = reader.readLine() ?: break
                    processClientPacket(session, line)
                }
            } catch (e: Exception) {
                // Client disconnected or read error
            } finally {
                val session = clientSessions.remove(sessionId)
                if (session != null) {
                    val clientName = session.username.ifEmpty { "Client" }
                    log("[DISCONNECT] $clientName meninggalkan server")
                    broadcastSystem(session.currentRoom, "🚪 $clientName telah keluar dari obrolan")
                    updateClientCount()
                    broadcastUserList(session.currentRoom)
                    broadcastRoomList()
                    try {
                        session.socket.close()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun processClientPacket(session: ClientSession, jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            when (json.optString("type")) {
                Protocol.TYPE_JOIN -> {
                    val username = json.optString("username", "Tamu")
                    val room = json.optString("room", "general")
                    val avatarIndex = json.optInt("avatarIndex", 0)

                    session.username = username
                    session.currentRoom = room
                    session.avatarIndex = avatarIndex

                    log("[JOIN] User '$username' masuk ke room #$room")

                    // Send updated room list & user list
                    broadcastRoomList()
                    broadcastUserList(room)

                    // Welcome system message in the room
                    broadcastSystem(room, "👋 $username telah bergabung ke room #$room")
                }

                Protocol.TYPE_SWITCH_ROOM -> {
                    val oldRoom = json.optString("oldRoom", session.currentRoom)
                    val newRoom = json.optString("newRoom", "general")
                    val username = session.username

                    session.currentRoom = newRoom
                    log("[SWITCH] $username berpindah dari #$oldRoom ke #$newRoom")

                    broadcastUserList(oldRoom)
                    broadcastUserList(newRoom)
                    broadcastRoomList()

                    broadcastSystem(oldRoom, "💨 $username berpindah ke room #$newRoom")
                    broadcastSystem(newRoom, "👋 $username masuk ke room #$newRoom")
                }

                Protocol.TYPE_MSG -> {
                    val msgId = json.optString("id", UUID.randomUUID().toString())
                    val roomId = json.optString("roomId", session.currentRoom)
                    val senderName = json.optString("senderName", session.username)
                    val senderAvatar = json.optInt("senderAvatarIndex", session.avatarIndex)
                    val content = json.optString("content", "")
                    val timestamp = json.optLong("timestamp", System.currentTimeMillis())
                    val replyToSender = json.optString("replyToSender").ifEmpty { null }
                    val replyToContent = json.optString("replyToContent").ifEmpty { null }

                    val chatMsg = ChatMessage(
                        id = msgId,
                        roomId = roomId,
                        senderName = senderName,
                        senderAvatarIndex = senderAvatar,
                        content = content,
                        timestamp = timestamp,
                        replyToSender = replyToSender,
                        replyToContent = replyToContent
                    )

                    log("[MSG] [#$roomId] $senderName: ${content.take(40)}")
                    broadcastToRoom(roomId, Protocol.createMsgPacket(chatMsg))

                    _serverState.value = _serverState.value.copy(
                        totalMessagesRelayed = _serverState.value.totalMessagesRelayed + 1
                    )

                    // Check for built-in bot commands
                    if (content.startsWith("/")) {
                        handleCommand(session, roomId, content)
                    }
                }

                Protocol.TYPE_REACTION -> {
                    val msgId = json.optString("msgId")
                    val roomId = json.optString("roomId")
                    val emoji = json.optString("emoji")
                    broadcastToRoom(roomId, jsonString)
                }

                Protocol.TYPE_CREATE_ROOM -> {
                    val id = json.optString("id")
                    val name = json.optString("name")
                    val topic = json.optString("topic")
                    val iconName = json.optString("iconName", "chat")
                    val isPrivate = json.optBoolean("isPrivate", false)
                    val password = json.optString("password", "")

                    val newRoom = RoomInfo(
                        id = id.ifBlank { name.lowercase().replace(" ", "-") },
                        name = name,
                        topic = topic,
                        iconName = iconName,
                        isPrivate = isPrivate,
                        password = password,
                        isDefault = false
                    )

                    if (rooms.none { it.id == newRoom.id }) {
                        rooms.add(newRoom)
                        log("[ROOM] ➕ Room baru dibuat: #${newRoom.name} ($topic)")
                        broadcastRoomList()
                        broadcastSystem(newRoom.id, "🎉 Room baru #${newRoom.name} berhasil dibuat!")
                    }
                }

                Protocol.TYPE_PING -> {
                    sendToSession(session, JSONObject().apply { put("type", Protocol.TYPE_PONG) }.toString())
                }
            }
        } catch (e: Exception) {
            log("[ERROR] Gagal memproses paket: ${e.localizedMessage}")
        }
    }

    private fun handleCommand(session: ClientSession, roomId: String, commandText: String) {
        serverScope.launch {
            delay(400) // Slight human-like pause
            val parts = commandText.trim().split(" ")
            val cmd = parts[0].lowercase()

            when (cmd) {
                "/help" -> {
                    val helpText = """
                        🤖 **Bantuan Perintah Server:**
                        • `/help` - Tampilkan daftar perintah
                        • `/info` - Status & statistik server
                        • `/joke` - Lelucon kocak dari bot
                        • `/roll` - Lempar dadu acak (1-100)
                        • `/ping` - Cek respons server
                    """.trimIndent()
                    broadcastBotMessage(roomId, "Bot Server", 1, helpText)
                }
                "/info" -> {
                    val state = _serverState.value
                    val info = "⚡ **Server Info:** Uptime: ${state.uptimeSeconds}s | Klien: ${state.connectedClientsCount} | Pesan: ${state.totalMessagesRelayed} | Total Room: ${rooms.size}"
                    broadcastBotMessage(roomId, "Bot Server", 1, info)
                }
                "/joke" -> {
                    val jokes = listOf(
                        "Kenapa programmer suka kopi hitam? Karena kalo manis nanti banyak bug-nya! ☕🐛",
                        "Kenapa router Wi-Fi galau? Karena sinyalnya sering diputus-sambungin! 📶💔",
                        "Database mana yang paling jago silat? MongoDB, karena kalo diserang langsung sharding! 🥋💥",
                        "Kenapa server tidak pernah kedinginan? Karena selalu pakai firewall! 🔥🛡️"
                    )
                    broadcastBotMessage(roomId, "Bot Bayu", 2, jokes.random())
                }
                "/roll" -> {
                    val rollResult = (1..100).random()
                    broadcastBotMessage(roomId, "Bot Server", 1, "🎲 ${session.username} melempar dadu dan mendapatkan angka: **$rollResult** / 100!")
                }
                "/ping" -> {
                    broadcastBotMessage(roomId, "Bot Server", 1, "🏓 Pong! Server ChatRoom beroperasi dengan kecepatan penuh.")
                }
            }
        }
    }

    private fun startBotCompanion() {
        botJob?.cancel()
        botJob = serverScope.launch {
            // Initial friendly welcome
            delay(1500)
            if (_serverState.value.botsEnabled && clientSessions.isNotEmpty()) {
                broadcastBotMessage(
                    "general",
                    "Bot Bayu",
                    2,
                    "Halo semua! Selamat datang di ChatRoom server lokal. Ketik `/help` untuk fitur seru! 🚀"
                )
            }
        }
    }

    fun broadcastAnnouncement(content: String) {
        log("[ANNOUNCEMENT] 📢 $content")
        val packet = Protocol.createAnnouncementPacket(content)
        clientSessions.values.forEach { session ->
            sendToSession(session, packet)
        }
        _serverState.value = _serverState.value.copy(
            totalMessagesRelayed = _serverState.value.totalMessagesRelayed + 1
        )
    }

    fun broadcastBotMessage(roomId: String, botName: String, avatarIndex: Int, text: String) {
        val chatMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            roomId = roomId,
            senderName = botName,
            senderAvatarIndex = avatarIndex,
            content = text,
            timestamp = System.currentTimeMillis()
        )
        broadcastToRoom(roomId, Protocol.createMsgPacket(chatMsg))
    }

    private fun broadcastToRoom(roomId: String, packet: String) {
        clientSessions.values.filter { it.currentRoom == roomId }.forEach { session ->
            sendToSession(session, packet)
        }
    }

    private fun broadcastSystem(roomId: String, content: String) {
        val packet = Protocol.createSystemPacket(roomId, content)
        broadcastToRoom(roomId, packet)
    }

    fun broadcastRoomList() {
        val packet = Protocol.createRoomListPacket(getRoomsWithCounts())
        clientSessions.values.forEach { session ->
            sendToSession(session, packet)
        }
    }

    private fun broadcastUserList(roomId: String) {
        val users = clientSessions.values
            .filter { it.currentRoom == roomId }
            .map {
                ChatUser(
                    id = it.id,
                    name = it.username.ifEmpty { "Pengguna" },
                    avatarIndex = it.avatarIndex,
                    status = "Online",
                    isHost = it.isHost,
                    currentRoom = it.currentRoom
                )
            }
        val packet = Protocol.createUserListPacket(roomId, users)
        broadcastToRoom(roomId, packet)
    }

    private fun getRoomsWithCounts(): List<RoomInfo> {
        return rooms.map { room ->
            val count = clientSessions.values.count { it.currentRoom == room.id }
            room.copy(memberCount = count)
        }
    }

    private fun sendToSession(session: ClientSession, packet: String) {
        try {
            session.writer.println(packet)
        } catch (_: Exception) {}
    }

    private fun updateClientCount() {
        _serverState.value = _serverState.value.copy(
            connectedClientsCount = clientSessions.size
        )
    }

    private fun startUptimeTracker() {
        timerJob?.cancel()
        timerJob = serverScope.launch {
            var seconds = 0L
            while (isActive && _serverState.value.isRunning) {
                delay(1000)
                seconds++
                _serverState.value = _serverState.value.copy(uptimeSeconds = seconds)
            }
        }
    }

    fun toggleBots() {
        val current = _serverState.value.botsEnabled
        _serverState.value = _serverState.value.copy(botsEnabled = !current)
        log("[SERVER] Bot pendamping sekarang: ${if (!current) "🟢 Aktif" else "⚪ Nonaktif"}")
    }

    fun stop() {
        log("[SERVER] 🛑 Menghentikan server...")
        timerJob?.cancel()
        botJob?.cancel()
        serverJob?.cancel()

        clientSessions.values.forEach {
            try {
                it.socket.close()
            } catch (_: Exception) {}
        }
        clientSessions.clear()

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        _serverState.value = _serverState.value.copy(
            isRunning = false,
            connectedClientsCount = 0,
            uptimeSeconds = 0
        )
        log("[SERVER] Server telah dihentikan")
    }

    fun log(message: String) {
        val time = timeFormat.format(Date())
        val entry = "[$time] $message"
        val updated = (_serverState.value.logs + entry).takeLast(60)
        _serverState.value = _serverState.value.copy(logs = updated)
    }
}
