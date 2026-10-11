package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChatServerEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ServerRepository(private val firestore: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance(firestore.app)

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserServers(): Flow<List<ChatServerEntry>> {
        val uid = requireUserId()
        val colRef = firestore.collection("users").document(uid).collection("servers")
        return colRef.snapshots().map { snapshot ->
            snapshot.toObjects(ChatServerEntry::class.java)
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun saveServerEntry(entry: ChatServerEntry): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = firestore.collection("users").document(uid).collection("servers").document(entry.id)
            val payload = hashMapOf<String, Any?>(
                "userId" to uid,
                "name" to entry.name,
                "host" to entry.host,
                "port" to entry.port,
                "hostedLocally" to entry.hostedLocally,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/${auth.currentUser?.uid}/servers/${entry.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteServerEntry(serverId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = firestore.collection("users").document(uid).collection("servers").document(serverId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/${auth.currentUser?.uid}/servers/$serverId")
            Result.failure(e)
        }
    }
}
