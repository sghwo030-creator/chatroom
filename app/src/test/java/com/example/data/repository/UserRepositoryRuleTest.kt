package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ChatServerEntry
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class UserRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveUserProfile_authenticated_succeeds() = runBlocking {
        val uid = signInTestUser("alice_${UUID.randomUUID()}@example.com")
        val userRepo = UserRepository(firestore)

        val result = withTimeout(5000L) {
            userRepo.saveUserProfile(displayName = "Alice", avatarIndex = 1, status = "Mabar")
        }
        assertTrue(result.isSuccess)

        val profile = withTimeout(5000L) {
            userRepo.observeUserProfile().first { it != null }
        }
        assertNotNull(profile)
        assertEquals("Alice", profile?.displayName)
    }

    @Test
    fun observeUserProfile_unauthenticated_fails() = runBlocking {
        val userRepo = UserRepository(firestore)
        auth.signOut()

        var caughtException = false
        try {
            withTimeout(3000L) {
                userRepo.observeUserProfile().first()
            }
        } catch (_: Exception) {
            caughtException = true
        }
        assertTrue(caughtException)
    }

    @Test
    fun serverRepository_saveAndObserve_succeeds() = runBlocking {
        val uid = signInTestUser("bob_${UUID.randomUUID()}@example.com")
        val serverRepo = ServerRepository(firestore)

        val serverId = "srv_${UUID.randomUUID().toString().take(8)}"
        val entry = ChatServerEntry(
            id = serverId,
            userId = uid,
            name = "Server Gaming",
            host = "192.168.1.100",
            port = 8888,
            hostedLocally = true
        )

        val saveResult = withTimeout(5000L) {
            serverRepo.saveServerEntry(entry)
        }
        if (saveResult.isFailure) {
            println("SAVE ERROR: " + saveResult.exceptionOrNull()?.message)
            saveResult.exceptionOrNull()?.printStackTrace()
        }
        assertTrue(saveResult.exceptionOrNull()?.message ?: "", saveResult.isSuccess)

        val list = withTimeout(5000L) {
            serverRepo.observeUserServers().first { it.isNotEmpty() }
        }
        assertTrue(list.any { it.id == serverId })
    }
}
