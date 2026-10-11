package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(private val firestore: FirebaseFirestore) {

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

    fun observeUserProfile(): Flow<UserProfile?> {
        val uid = requireUserId()
        val docRef = firestore.collection("users").document(uid)
        return docRef.snapshots().map { snapshot ->
            if (snapshot.exists()) {
                snapshot.toObject(UserProfile::class.java)
            } else {
                null
            }
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun saveUserProfile(displayName: String, avatarIndex: Int, status: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val email = auth.currentUser?.email ?: ""
            val photoUrl = auth.currentUser?.photoUrl?.toString()
            val docRef = firestore.collection("users").document(uid)

            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val payload = hashMapOf<String, Any?>(
                    "userId" to uid,
                    "displayName" to displayName,
                    "email" to email,
                    "photoUrl" to photoUrl,
                    "status" to status,
                    "avatarIndex" to avatarIndex,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }

                docRef.set(payload).await()
            } else {
                val updates = hashMapOf<String, Any?>(
                    "displayName" to displayName,
                    "status" to status,
                    "avatarIndex" to avatarIndex,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }

                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}")
            Result.failure(e)
        }
    }
}
