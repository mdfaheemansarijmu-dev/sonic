package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SonicLocalRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createPlaylist_andObserveMyPlaylists_succeedsForOwner() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = SonicLocalRepository(firestore, auth)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createPlaylist(
                ownerName = "Alice Rivera",
                title = "Silver Lake Sunset Mix",
                description = "Dream pop and garage rock from local bands",
                neighborhood = "Silver Lake, LA",
                moodTag = "Golden Hour",
                coverTheme = "amber",
                isShared = false,
                trackIds = listOf("curated_1", "curated_2")
            )
        }
        assertTrue("Expected playlist creation to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val playlistId = createResult.getOrThrow()

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMyPlaylists(aliceUid).first { list -> list.any { it.id == playlistId } }
        }
        assertTrue(emitted.any { it.id == playlistId && it.title == "Silver Lake Sunset Mix" })
    }

    @Test
    fun getPlaylistById_privatePlaylistCrossUser_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = SonicLocalRepository(firestore, auth)
        val playlistId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createPlaylist(
                ownerName = "Alice",
                title = "Secret Demo Tape",
                description = "Private tracks",
                neighborhood = "Echo Park",
                moodTag = "Late Night",
                coverTheme = "violet",
                isShared = false,
                trackIds = listOf("curated_1")
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = SonicLocalRepository(firestore, auth)
        val bobReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getPlaylistById(playlistId)
        }
        assertTrue("Expected cross-user private read to fail", bobReadResult.isFailure)
        val ex = bobReadResult.exceptionOrNull() as? FirebaseFirestoreException
        assertNotNull(ex)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
    }

    @Test
    fun sharedPlaylist_canBeObservedAndLikedByFriend() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = SonicLocalRepository(firestore, auth)
        val playlistId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createPlaylist(
                ownerName = "Alice",
                title = "Williamsburg Indie Night",
                description = "Shared with friends",
                neighborhood = "Williamsburg, NYC",
                moodTag = "Rooftop",
                coverTheme = "crimson",
                isShared = true,
                trackIds = listOf("curated_1")
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = SonicLocalRepository(firestore, auth)
        val sharedList = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeSharedPlaylists().first { list -> list.any { it.id == playlistId } }
        }
        val target = sharedList.first { it.id == playlistId }
        val likeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.likeSharedPlaylist(playlistId, target.likesCount)
        }
        assertTrue("Expected friend like (+1) to succeed", likeResult.isSuccess)
    }

    @Test
    fun observeSharedPlaylists_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = SonicLocalRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeSharedPlaylists().first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated caller")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@soniclocal.test"
        const val BOB_EMAIL = "bob@soniclocal.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
