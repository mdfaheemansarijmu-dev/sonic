package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.OnlineAudioPlayer
import com.example.audio.PlayerState
import com.example.data.ArtistTrack
import com.example.data.LocalArtistSpotlight
import com.example.data.LocalMusicCatalog
import com.example.data.Playlist
import com.example.data.SavedTrack
import com.example.data.SonicLocalRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class SonicLocalViewModel(
    private val repository: SonicLocalRepository,
    private val audioPlayer: OnlineAudioPlayer,
    val currentUserId: String,
    val currentUserName: String
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = audioPlayer.state

    private val _selectedCity = MutableStateFlow("All Cities")
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    private val _selectedGenre = MutableStateFlow("All Scenes")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _discoveredSharePlaylist = MutableStateFlow<Playlist?>(null)
    val discoveredSharePlaylist: StateFlow<Playlist?> = _discoveredSharePlaylist.asStateFlow()

    val myPlaylistsState: StateFlow<UiState<List<Playlist>>> = repository.observeMyPlaylists(currentUserId)
        .map<List<Playlist>, UiState<List<Playlist>>> { list ->
            val sorted = list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            UiState.Success(sorted)
        }
        .catch { error ->
            Log.w(TAG, "Error observing user playlists", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load your playlists"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val sharedPlaylistsState: StateFlow<UiState<List<Playlist>>> = repository.observeSharedPlaylists()
        .map<List<Playlist>, UiState<List<Playlist>>> { remoteShared ->
            val combined = (remoteShared + LocalMusicCatalog.featuredCommunityPlaylists)
                .distinctBy { it.id }
                .sortedByDescending { it.likesCount }
            UiState.Success(combined)
        }
        .catch { error ->
            Log.w(TAG, "Error observing shared playlists", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load shared playlists"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(LocalMusicCatalog.featuredCommunityPlaylists)
        )

    val communityTracksState: StateFlow<UiState<List<ArtistTrack>>> = repository.observePublicArtistTracks()
        .map<List<ArtistTrack>, UiState<List<ArtistTrack>>> { remoteTracks ->
            val combined = (remoteTracks.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } + LocalMusicCatalog.curatedTracks)
                .distinctBy { it.id }
            UiState.Success(combined)
        }
        .catch { error ->
            Log.w(TAG, "Error observing community tracks", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load community tracks"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(LocalMusicCatalog.curatedTracks)
        )

    val savedTracksState: StateFlow<UiState<List<SavedTrack>>> = repository.observeSavedTracks(currentUserId)
        .map<List<SavedTrack>, UiState<List<SavedTrack>>> { saved ->
            val sorted = saved.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            UiState.Success(sorted)
        }
        .catch { error ->
            Log.w(TAG, "Error observing saved tracks", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load saved tracks"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun selectCity(city: String) {
        _selectedCity.value = city
    }

    fun selectGenre(genre: String) {
        _selectedGenre.value = genre
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun clearDiscoveredSharePlaylist() {
        _discoveredSharePlaylist.value = null
    }

    fun resolveAllTracks(): List<ArtistTrack> {
        return (communityTracksState.value as? UiState.Success)?.data ?: LocalMusicCatalog.curatedTracks
    }

    fun playTrackOnline(track: ArtistTrack, customQueue: List<ArtistTrack> = emptyList()) {
        val queue = if (customQueue.isNotEmpty()) customQueue else resolveAllTracks()
        audioPlayer.playTrack(track, queue)
        if (!track.id.startsWith("curated_")) {
            viewModelScope.launch {
                repository.incrementTrackPlays(track.id, track.playsCount)
            }
        }
    }

    fun playPlaylistOnline(playlist: Playlist) {
        val allTracks = resolveAllTracks()
        val playlistTracks = playlist.trackIds.mapNotNull { id -> allTracks.find { it.id == id } }
            .ifEmpty { LocalMusicCatalog.curatedTracks.take(4) }
        audioPlayer.playTrack(playlistTracks.first(), playlistTracks)
        _statusBanner.value = "Streaming \"${playlist.title}\" (${playlistTracks.size} tracks)"
    }

    fun playArtistSpotlight(spotlight: LocalArtistSpotlight) {
        if (spotlight.tracks.isNotEmpty()) {
            audioPlayer.playTrack(spotlight.tracks.first(), spotlight.tracks)
            _statusBanner.value = "Streaming ${spotlight.artistName} (${spotlight.city})"
        }
    }

    fun togglePlayPause() = audioPlayer.togglePlayPause()
    fun skipNext() = audioPlayer.skipNext()
    fun skipPrevious() = audioPlayer.skipPrevious()
    fun seekToFraction(fraction: Float) = audioPlayer.seekToFraction(fraction)
    fun toggleShuffle() = audioPlayer.toggleShuffle()
    fun cycleRepeatMode() = audioPlayer.cycleRepeatMode()

    fun toggleSaveTrack(track: ArtistTrack) {
        val currentSaved = (savedTracksState.value as? UiState.Success)?.data ?: emptyList()
        val alreadySaved = currentSaved.any { it.trackId == track.id || it.id == track.id }
        viewModelScope.launch {
            if (alreadySaved) {
                repository.removeSavedTrack(track.id)
                    .onSuccess { _statusBanner.value = "Removed \"${track.trackTitle}\" from My Crate" }
                    .onFailure { _statusBanner.value = it.localizedMessage ?: "Could not remove track" }
            } else {
                repository.saveTrackToLibrary(track)
                    .onSuccess { _statusBanner.value = "Saved \"${track.trackTitle}\" to My Crate" }
                    .onFailure { _statusBanner.value = it.localizedMessage ?: "Could not save track" }
            }
        }
    }

    fun createCuratedPlaylist(
        title: String,
        description: String,
        neighborhood: String,
        moodTag: String,
        coverTheme: String,
        isShared: Boolean,
        trackIds: List<String>
    ) {
        if (title.isBlank()) {
            _statusBanner.value = "Please enter a playlist title"
            return
        }
        viewModelScope.launch {
            repository.createPlaylist(
                ownerName = currentUserName,
                title = title,
                description = description,
                neighborhood = neighborhood,
                moodTag = moodTag,
                coverTheme = coverTheme,
                isShared = isShared,
                trackIds = trackIds
            ).onSuccess {
                _statusBanner.value = if (isShared) {
                    "Created & shared \"$title\" with friends!"
                } else {
                    "Created private playlist \"$title\""
                }
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to create playlist"
            }
        }
    }

    fun addTrackToPlaylist(playlist: Playlist, track: ArtistTrack) {
        if (playlist.ownerId != currentUserId) {
            _statusBanner.value = "You can only edit playlists you created"
            return
        }
        val updated = (playlist.trackIds + track.id).distinct()
        viewModelScope.launch {
            repository.updatePlaylistTracks(playlist.id, updated)
                .onSuccess { _statusBanner.value = "Added \"${track.trackTitle}\" to ${playlist.title}" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to update playlist" }
        }
    }

    fun removeTrackFromPlaylist(playlist: Playlist, trackId: String) {
        if (playlist.ownerId != currentUserId) return
        val updated = playlist.trackIds.filterNot { it == trackId }
        viewModelScope.launch {
            repository.updatePlaylistTracks(playlist.id, updated)
                .onSuccess { _statusBanner.value = "Removed track from ${playlist.title}" }
        }
    }

    fun toggleSharePlaylist(playlist: Playlist) {
        if (playlist.ownerId != currentUserId) return
        val newSharedState = !playlist.isShared
        viewModelScope.launch {
            repository.togglePlaylistShareState(playlist.id, newSharedState)
                .onSuccess {
                    _statusBanner.value = if (newSharedState) {
                        "\"${playlist.title}\" is now live for friends! Code: ${playlist.shareCode}"
                    } else {
                        "\"${playlist.title}\" is now private"
                    }
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Could not update sharing" }
        }
    }

    fun likeSharedPlaylist(playlist: Playlist) {
        if (playlist.id.startsWith("featured_")) {
            _statusBanner.value = "Sent love to ${playlist.title}!"
            return
        }
        viewModelScope.launch {
            repository.likeSharedPlaylist(playlist.id, playlist.likesCount)
                .onSuccess { _statusBanner.value = "Liked \"${playlist.title}\"!" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Could not like playlist" }
        }
    }

    fun deleteMyPlaylist(playlist: Playlist) {
        if (playlist.ownerId != currentUserId) return
        viewModelScope.launch {
            repository.deletePlaylist(playlist.id)
                .onSuccess { _statusBanner.value = "Deleted \"${playlist.title}\"" }
        }
    }

    fun lookupFriendPlaylistByCode(shareCode: String) {
        val normalized = shareCode.trim().uppercase()
        if (normalized.isBlank()) {
            _statusBanner.value = "Enter a friend's invite code (e.g. SONIC-NYC1)"
            return
        }
        val featuredMatch = LocalMusicCatalog.featuredCommunityPlaylists.find {
            it.shareCode.equals(normalized, ignoreCase = true)
        }
        if (featuredMatch != null) {
            _discoveredSharePlaylist.value = featuredMatch
            _statusBanner.value = "Found friend playlist: ${featuredMatch.title}!"
            return
        }
        viewModelScope.launch {
            repository.findSharedPlaylistByCode(normalized)
                .onSuccess { found ->
                    if (found != null) {
                        _discoveredSharePlaylist.value = found
                        _statusBanner.value = "Found friend playlist: ${found.title}!"
                    } else {
                        _statusBanner.value = "No shared playlist found with code $normalized"
                    }
                }
                .onFailure {
                    _statusBanner.value = it.localizedMessage ?: "Failed to look up share code"
                }
        }
    }

    fun publishArtistTrackOnline(
        artistName: String,
        trackTitle: String,
        city: String,
        genre: String,
        streamUrl: String,
        bio: String,
        durationSec: Long
    ) {
        if (artistName.isBlank() || trackTitle.isBlank()) {
            _statusBanner.value = "Artist name and track title are required"
            return
        }
        val finalUrl = streamUrl.trim().ifBlank {
            "soniclocal://synth/${genre.lowercase().replace(" ", "_")}"
        }
        viewModelScope.launch {
            repository.publishArtistTrack(
                artistName = artistName,
                trackTitle = trackTitle,
                city = city.ifBlank { "Local Underground Scene" },
                genre = genre,
                streamUrl = finalUrl,
                bio = bio.ifBlank { "Independent artist streaming live on SonicLocal." },
                durationSec = durationSec
            ).onSuccess {
                _statusBanner.value = "Published \"$trackTitle\" by $artistName for online streaming!"
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to publish track"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }

    private companion object {
        const val TAG = "SonicLocalVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
