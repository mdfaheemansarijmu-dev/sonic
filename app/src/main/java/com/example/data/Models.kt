package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Playlist(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val title: String = "",
    val description: String = "",
    val neighborhood: String = "",
    val moodTag: String = "",
    val coverTheme: String = "amber",
    @get:PropertyName("isShared") @set:PropertyName("isShared")
    var isShared: Boolean = true,
    val shareCode: String = "",
    val trackIds: List<String> = emptyList(),
    val likesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ArtistTrack(
    @DocumentId val id: String = "",
    val uploaderId: String = "",
    val artistName: String = "",
    val trackTitle: String = "",
    val city: String = "",
    val genre: String = "Indie Rock",
    val streamUrl: String = "",
    val bio: String = "",
    val durationSec: Long = 180L,
    @get:PropertyName("isPublic") @set:PropertyName("isPublic")
    var isPublic: Boolean = true,
    val playsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavedTrack(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackId: String = "",
    val trackTitle: String = "",
    val artistName: String = "",
    val city: String = "",
    val genre: String = "",
    val streamUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class LocalArtistSpotlight(
    val id: String,
    val artistName: String,
    val city: String,
    val neighborhood: String,
    val primaryGenre: String,
    val bio: String,
    val monthlyListeners: Int,
    val nextVenue: String,
    val accentHex: Long,
    val tracks: List<ArtistTrack>
)
