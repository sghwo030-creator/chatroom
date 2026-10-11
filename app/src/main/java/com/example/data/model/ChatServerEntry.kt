package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ChatServerEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val host: String = "127.0.0.1",
    val port: Int = 8888,
    val hostedLocally: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
