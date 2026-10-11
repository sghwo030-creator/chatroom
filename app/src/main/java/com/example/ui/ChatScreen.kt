package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ChatBubble
import com.example.ui.components.ConnectServerDialog
import com.example.ui.components.CreateRoomDialog
import com.example.ui.components.ProfileDialog
import com.example.ui.components.RoomDrawerContent
import com.example.ui.components.ServerDashboardSheet
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var showServerDashboard by remember { mutableStateOf(false) }
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var showConnectDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val currentRoom = uiState.rooms.find { it.id == uiState.currentRoomId }
        ?: uiState.rooms.firstOrNull()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CyberBg,
                modifier = Modifier.width(310.dp)
            ) {
                RoomDrawerContent(
                    currentRoomId = uiState.currentRoomId,
                    rooms = uiState.rooms,
                    unreadCounts = uiState.unreadCounts,
                    roomUsers = uiState.roomUsers,
                    serverState = uiState.serverState,
                    currentUser = uiState.currentUsername,
                    currentAvatarIndex = uiState.currentAvatarIndex,
                    currentStatus = uiState.currentStatus,
                    userEmail = uiState.userEmail,
                    onSelectRoom = { roomId ->
                        viewModel.selectRoom(roomId)
                        scope.launch { drawerState.close() }
                    },
                    onCreateRoomClick = {
                        scope.launch { drawerState.close() }
                        showCreateRoomDialog = true
                    },
                    onOpenServerDashboard = {
                        scope.launch { drawerState.close() }
                        showServerDashboard = true
                    },
                    onEditProfileClick = {
                        scope.launch { drawerState.close() }
                        showProfileDialog = true
                    },
                    onSignOutClick = {
                        scope.launch { drawerState.close() }
                        onSignOut()
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(CyberBg),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "#${currentRoom?.name ?: "general"}",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // Member count chip
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CyberCyan.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${uiState.roomUsers.size} online",
                                        color = CyberCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = currentRoom?.topic?.ifEmpty { "Ruang obrolan ChatRoom" } ?: "Ruang obrolan",
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("btn_open_drawer")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu Ruang",
                                tint = CyberCyan
                            )
                        }
                    },
                    actions = {
                        // Server Status Pill Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CyberSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (uiState.serverState.isRunning) CyberEmerald else CyberBorder
                            ),
                            modifier = Modifier
                                .clickable { showServerDashboard = true }
                                .padding(end = 4.dp)
                                .testTag("btn_top_server_status")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.serverState.isRunning) CyberEmerald else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (uiState.serverState.isRunning) "Server On" else "Server Off",
                                    color = if (uiState.serverState.isRunning) CyberEmerald else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // More Menu
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.testTag("btn_more_options")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Menu Lainnya",
                                    tint = TextSecondary
                                )
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                modifier = Modifier.background(CyberSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Panel Kontrol Server", color = TextPrimary) },
                                    onClick = {
                                        showMoreMenu = false
                                        showServerDashboard = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Buat Ruang Baru", color = TextPrimary) },
                                    onClick = {
                                        showMoreMenu = false
                                        showCreateRoomDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit Profil Saya", color = TextPrimary) },
                                    onClick = {
                                        showMoreMenu = false
                                        showProfileDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Keluar Akun (Sign Out)", color = TextSecondary) },
                                    onClick = {
                                        showMoreMenu = false
                                        onSignOut()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Bersihkan Chat Ruang Ini", color = CyberAmber) },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.clearRoomHistory(uiState.currentRoomId)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CyberBg
                    )
                )
            },
            containerColor = CyberBg
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
            ) {
                // Quick Room Selector Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(uiState.rooms) { room ->
                        val isSelected = room.id == uiState.currentRoomId
                        val unread = uiState.unreadCounts[room.id] ?: 0

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else CyberSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CyberCyan else CyberBorder
                            ),
                            modifier = Modifier
                                .clickable { viewModel.selectRoom(room.id) }
                                .testTag("quick_room_${room.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${room.name}",
                                    color = if (isSelected) CyberCyan else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                if (unread > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(CyberCyan)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CyberSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier
                                .clickable { showCreateRoomDialog = true }
                                .testTag("quick_btn_add_room")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Tambah Ruang",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Room", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Global Announcement Banner (if any)
                AnimatedVisibility(
                    visible = uiState.activeAnnouncement != null,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    uiState.activeAnnouncement?.let { ann ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CyberAmber.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberAmber.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = CyberAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = ann,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.dismissAnnouncement() },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Tutup Pengumuman",
                                        tint = TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Chat Messages List
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (uiState.messages.isEmpty()) {
                        // Empty State Graphic
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.1f))
                                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Selamat Datang di #${currentRoom?.name ?: "general"}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Belum ada pesan di ruang ini. Kirim pesan pertama atau coba ketik perintah /help atau /joke!",
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            items(uiState.messages, key = { it.id }) { msg ->
                                ChatBubble(
                                    message = msg,
                                    onReply = { viewModel.setReplyingTo(it) },
                                    onReaction = { msgId, emoji -> viewModel.addReaction(msgId, emoji) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                // Reply Preview Bar
                AnimatedVisibility(visible = uiState.replyingTo != null) {
                    uiState.replyingTo?.let { reply ->
                        Surface(
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                            color = CyberSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(24.dp)
                                            .background(CyberCyan)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Membalas ${reply.senderName}",
                                            color = CyberCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = reply.content,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.setReplyingTo(null) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Batal Balas",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Chat Input Field Bar
                Surface(
                    color = CyberSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .navigationBarsPadding()
                    ) {
                        // Quick Emoji Suggestions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val emojiSuggestions = listOf("👋", "👍", "❤️", "😂", "🔥", "🚀", "☕")
                            emojiSuggestions.forEach { emoji ->
                                Text(
                                    text = emoji,
                                    fontSize = 16.sp,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            inputText += emoji
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }

                        // Message Text Box + Send Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = {
                                    Text(
                                        "Pesan ke #${currentRoom?.name ?: "general"}...",
                                        color = TextMuted,
                                        fontSize = 14.sp
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = CyberBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = CyberBg,
                                    unfocusedContainerColor = CyberBg
                                ),
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 4,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_chat_message")
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        viewModel.sendMessage(inputText)
                                        inputText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (inputText.isNotBlank()) CyberCyan else CyberSurfaceVariant
                                    )
                                    .testTag("btn_send_message")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Kirim",
                                    tint = if (inputText.isNotBlank()) Color.Black else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialogs
    if (showServerDashboard) {
        ServerDashboardSheet(
            serverState = uiState.serverState,
            clientState = uiState.clientState,
            onDismiss = { showServerDashboard = false },
            onToggleServer = { isStart ->
                if (isStart) viewModel.startServerAndConnect(8888) else viewModel.stopServer()
            },
            onToggleBots = { viewModel.toggleServerBots() },
            onBroadcastAnnouncement = { viewModel.broadcastServerAnnouncement(it) },
            onOpenConnectDialog = {
                showServerDashboard = false
                showConnectDialog = true
            }
        )
    }

    if (showCreateRoomDialog) {
        CreateRoomDialog(
            onDismiss = { showCreateRoomDialog = false },
            onCreate = { name, topic, iconName, isPrivate, password ->
                viewModel.createRoom(name, topic, iconName, isPrivate, password)
            }
        )
    }

    if (showConnectDialog) {
        ConnectServerDialog(
            currentAddress = uiState.clientState.serverAddress,
            onDismiss = { showConnectDialog = false },
            onConnect = { host, port ->
                viewModel.connectToExternalServer(host, port)
            }
        )
    }

    if (showProfileDialog) {
        ProfileDialog(
            currentName = uiState.currentUsername,
            currentAvatarIndex = uiState.currentAvatarIndex,
            currentStatus = uiState.currentStatus,
            onDismiss = { showProfileDialog = false },
            onSave = { name, avatarIndex, status ->
                viewModel.updateProfile(name, avatarIndex, status)
            }
        )
    }
}
