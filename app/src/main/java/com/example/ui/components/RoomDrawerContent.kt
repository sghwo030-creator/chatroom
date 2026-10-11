package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatUser
import com.example.data.model.RoomInfo
import com.example.data.model.ServerState
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

@Composable
fun RoomDrawerContent(
    currentRoomId: String,
    rooms: List<RoomInfo>,
    unreadCounts: Map<String, Int>,
    roomUsers: List<ChatUser>,
    serverState: ServerState,
    currentUser: String,
    currentAvatarIndex: Int,
    currentStatus: String,
    userEmail: String = "",
    onSelectRoom: (String) -> Unit,
    onCreateRoomClick: () -> Unit,
    onOpenServerDashboard: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSignOutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val defaultRooms = rooms.filter { it.isDefault }
    val customRooms = rooms.filter { !it.isDefault }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(310.dp)
            .background(CyberBg)
            .padding(top = 16.dp, bottom = 12.dp)
    ) {
        // Server Info Header Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            shape = RoundedCornerShape(16.dp),
            color = CyberSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ChatRoom Server",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (serverState.isRunning) CyberEmerald else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (serverState.isRunning) "Server Host Aktif" else "Server Nonaktif",
                                    color = if (serverState.isRunning) CyberEmerald else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onOpenServerDashboard,
                        modifier = Modifier.size(32.dp).testTag("open_server_dashboard_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan Server",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Server Address badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenServerDashboard() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${serverState.hostIp}:${serverState.port}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "Kelola >",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable Room List & Active Users
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            // Section 1: Default Rooms
            item {
                Text(
                    text = "RUANG UTAMA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                )
            }

            items(defaultRooms) { room ->
                RoomItemRow(
                    room = room,
                    isSelected = room.id == currentRoomId,
                    unreadCount = unreadCounts[room.id] ?: 0,
                    onClick = { onSelectRoom(room.id) }
                )
            }

            // Section 2: Custom Rooms
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RUANG KUSTOM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onCreateRoomClick() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("btn_drawer_create_room")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Buat",
                            tint = CyberCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Buat",
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (customRooms.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada ruang kustom. Buat sekarang!",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 6.dp)
                    )
                }
            } else {
                items(customRooms) { room ->
                    RoomItemRow(
                        room = room,
                        isSelected = room.id == currentRoomId,
                        unreadCount = unreadCounts[room.id] ?: 0,
                        onClick = { onSelectRoom(room.id) }
                    )
                }
            }

            // Section 3: Online Users in Current Room
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = CyberBorder.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "ANGGOTA DI #${currentRoomId.uppercase()} (${roomUsers.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            if (roomUsers.isEmpty()) {
                item {
                    Text(
                        text = "Hanya Anda di ruang ini saat ini",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            } else {
                items(roomUsers) { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarHelper.Avatar(
                            name = user.name,
                            avatarIndex = user.avatarIndex,
                            size = 28.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.name,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (user.isHost) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("👑", fontSize = 11.sp)
                                }
                            }
                            Text(
                                text = user.status,
                                color = TextMuted,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CyberEmerald)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Profile footer card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            shape = RoundedCornerShape(14.dp),
            color = CyberSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditProfileClick() }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarHelper.Avatar(
                    name = currentUser,
                    avatarIndex = currentAvatarIndex,
                    size = 38.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentUser,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (userEmail.isNotBlank()) userEmail else currentStatus,
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
                IconButton(
                    onClick = onEditProfileClick,
                    modifier = Modifier.size(28.dp).testTag("btn_edit_profile")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profil",
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onSignOutClick,
                    modifier = Modifier.size(28.dp).testTag("btn_sign_out")
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Keluar Akun",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomItemRow(
    room: RoomInfo,
    isSelected: Boolean,
    unreadCount: Int,
    onClick: () -> Unit
) {
    val icon = when (room.iconName) {
        "gamepad" -> Icons.Default.Gamepad
        "code" -> Icons.Default.Code
        "music" -> Icons.Default.MusicNote
        "coffee" -> Icons.Default.Coffee
        "lock" -> Icons.Default.Lock
        else -> Icons.Default.Chat
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) CyberCyan.copy(alpha = 0.15f) else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() }
            .testTag("room_item_${room.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) CyberCyan else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${room.name}",
                        color = if (isSelected) CyberCyan else TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    if (room.isPrivate) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Private",
                            tint = TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                if (room.topic.isNotEmpty()) {
                    Text(
                        text = room.topic,
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            if (unreadCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = CyberCyan
                ) {
                    Text(
                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
