package com.example.data.model

data class RoomInfo(
    val id: String,
    val name: String,
    val topic: String = "",
    val iconName: String = "chat",
    val isPrivate: Boolean = false,
    val password: String = "",
    val memberCount: Int = 0,
    val isDefault: Boolean = false
) {
    companion object {
        fun defaultRooms(): List<RoomInfo> = listOf(
            RoomInfo(
                id = "general",
                name = "general",
                topic = "Ruang obrolan umum & santai untuk semua orang",
                iconName = "chat",
                isDefault = true
            ),
            RoomInfo(
                id = "gaming",
                name = "gaming",
                topic = "Mabar, rekomendasi game, & turnamen",
                iconName = "gamepad",
                isDefault = true
            ),
            RoomInfo(
                id = "tech",
                name = "tech",
                topic = "Koding, AI, gadget, & diskusi teknologi",
                iconName = "code",
                isDefault = true
            ),
            RoomInfo(
                id = "music",
                name = "music",
                topic = "Playlist favorit, lagu baru, & rekomendasi musik",
                iconName = "music",
                isDefault = true
            ),
            RoomInfo(
                id = "santai",
                name = "santai",
                topic = "Kopi santai, curhat hangat, & meme seru",
                iconName = "coffee",
                isDefault = true
            )
        )
    }
}
