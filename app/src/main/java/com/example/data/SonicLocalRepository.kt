package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SonicLocalRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        FirebaseAuth.getInstance()
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before writing to Firestore.")
    }

    private fun Query.observeSnapshots(): Flow<QuerySnapshot> = callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot)
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeMyPlaylists(userId: String): Flow<List<Playlist>> {
        val path = "playlists"
        return db.collection(path)
            .whereEqualTo("ownerId", userId)
            .observeSnapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Playlist::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeSharedPlaylists(): Flow<List<Playlist>> {
        val path = "playlists"
        return db.collection(path)
            .whereEqualTo("isShared", true)
            .observeSnapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Playlist::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observePublicArtistTracks(): Flow<List<ArtistTrack>> {
        val path = "artist_tracks"
        return db.collection(path)
            .whereEqualTo("isPublic", true)
            .observeSnapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ArtistTrack::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeSavedTracks(userId: String): Flow<List<SavedTrack>> {
        val path = "users/$userId/saved_tracks"
        return db.collection("users").document(userId).collection("saved_tracks")
            .observeSnapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(SavedTrack::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun getPlaylistById(playlistId: String): Result<Playlist?> = runCatching {
        val docRef = db.collection("playlists").document(playlistId)
        try {
            val snapshot = docRef.get().await()
            snapshot.toObject(Playlist::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun findSharedPlaylistByCode(shareCode: String): Result<Playlist?> = runCatching {
        val normalized = shareCode.trim().uppercase()
        val path = "playlists"
        try {
            val querySnapshot = db.collection(path)
                .whereEqualTo("isShared", true)
                .whereEqualTo("shareCode", normalized)
                .get()
                .await()
            querySnapshot.documents.firstOrNull()?.toObject(
                Playlist::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun createPlaylist(
        ownerName: String,
        title: String,
        description: String,
        neighborhood: String,
        moodTag: String,
        coverTheme: String,
        isShared: Boolean,
        trackIds: List<String>
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = "pl_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val shareCode = "SONIC-" + UUID.randomUUID().toString().uppercase().replace("-", "").take(4)
        val docRef = db.collection("playlists").document(docId)
        val payload = mapOf(
            "ownerId" to uid,
            "ownerName" to ownerName.trim().take(80).ifBlank { "Local Curator" },
            "title" to title.trim().take(100),
            "description" to description.trim().take(300),
            "neighborhood" to neighborhood.trim().take(80).ifBlank { "Downtown Arts District" },
            "moodTag" to moodTag.trim().take(40).ifBlank { "Indie Discovery" },
            "coverTheme" to if (coverTheme in listOf("amber", "crimson", "emerald", "violet", "cyan")) coverTheme else "amber",
            "isShared" to isShared,
            "shareCode" to shareCode,
            "trackIds" to trackIds.take(50),
            "likesCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updatePlaylistTracks(playlistId: String, updatedTrackIds: List<String>): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("playlists").document(playlistId)
        try {
            docRef.update(
                mapOf(
                    "trackIds" to updatedTrackIds.distinct().take(50),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun togglePlaylistShareState(playlistId: String, isShared: Boolean): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("playlists").document(playlistId)
        try {
            docRef.update(
                mapOf(
                    "isShared" to isShared,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun likeSharedPlaylist(playlistId: String, currentLikes: Long): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("playlists").document(playlistId)
        try {
            docRef.update(
                mapOf(
                    "likesCount" to (currentLikes + 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deletePlaylist(playlistId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("playlists").document(playlistId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun publishArtistTrack(
        artistName: String,
        trackTitle: String,
        city: String,
        genre: String,
        streamUrl: String,
        bio: String,
        durationSec: Long = 195L
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = "trk_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("artist_tracks").document(docId)
        val validGenres = listOf(
            "Indie Rock", "Lo-Fi Beats", "Neo-Soul",
            "Synthwave", "Folk Acoustic", "Underground Hip-Hop"
        )
        val safeGenre = if (genre in validGenres) genre else "Indie Rock"
        val payload = mapOf(
            "uploaderId" to uid,
            "artistName" to artistName.trim().take(80),
            "trackTitle" to trackTitle.trim().take(100),
            "city" to city.trim().take(80),
            "genre" to safeGenre,
            "streamUrl" to streamUrl.trim().take(500),
            "bio" to bio.trim().take(300),
            "durationSec" to durationSec.coerceIn(10L, 1200L),
            "isPublic" to true,
            "playsCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun incrementTrackPlays(trackId: String, currentPlays: Long): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("artist_tracks").document(trackId)
        try {
            docRef.update(
                mapOf(
                    "playsCount" to (currentPlays + 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun saveTrackToLibrary(track: ArtistTrack): Result<Unit> = runCatching {
        val uid = requireUserId()
        val safeTrackId = track.id.ifBlank { "trk_default" }
        val docRef = db.collection("users").document(uid).collection("saved_tracks").document(safeTrackId)
        val payload = mapOf(
            "userId" to uid,
            "trackId" to safeTrackId,
            "trackTitle" to track.trackTitle.take(100),
            "artistName" to track.artistName.take(80),
            "city" to track.city.take(80),
            "genre" to track.genre.take(40),
            "streamUrl" to track.streamUrl.take(500),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun removeSavedTrack(trackId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("saved_tracks").document(trackId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
