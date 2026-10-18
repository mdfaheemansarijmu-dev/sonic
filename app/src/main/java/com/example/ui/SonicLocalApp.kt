package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.audio.OnlineAudioPlayer
import com.example.auth.signOutUser
import com.example.data.ArtistTrack
import com.example.data.LocalArtistSpotlight
import com.example.data.LocalMusicCatalog
import com.example.data.Playlist
import com.example.data.SavedTrack
import com.example.data.SonicLocalRepository
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.StageViolet
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.VinylCoral
import com.google.firebase.firestore.FirebaseFirestore

enum class SonicTab(val label: String, val icon: ImageVector, val testTag: String) {
    DISCOVER("Discover", Icons.Default.Explore, "nav_tab_discover"),
    PLAYLISTS("Friend Mixes", Icons.Default.People, "nav_tab_playlists"),
    PUBLISH("Publish", Icons.Default.CloudUpload, "nav_tab_publish"),
    CRATE("My Crate", Icons.Default.LibraryMusic, "nav_tab_crate")
}

@Composable
fun SonicLocalFeatureRoot(
    currentUserId: String,
    currentUserName: String,
    onSignedOut: () -> Unit
) {
    val viewModel: SonicLocalViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SonicLocalViewModel(
                    repository = SonicLocalRepository(db),
                    audioPlayer = OnlineAudioPlayer(),
                    currentUserId = currentUserId,
                    currentUserName = currentUserName
                )
            }
        }
    )

    SonicLocalMainScreen(
        viewModel = viewModel,
        onSignedOut = onSignedOut
    )
}

@Composable
fun SonicLocalMainScreen(
    viewModel: SonicLocalViewModel,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val discoveredSharePlaylist by viewModel.discoveredSharePlaylist.collectAsStateWithLifecycle()

    val myPlaylistsState by viewModel.myPlaylistsState.collectAsStateWithLifecycle()
    val sharedPlaylistsState by viewModel.sharedPlaylistsState.collectAsStateWithLifecycle()
    val communityTracksState by viewModel.communityTracksState.collectAsStateWithLifecycle()
    val savedTracksState by viewModel.savedTracksState.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(SonicTab.DISCOVER) }
    var isFullPlayerOpen by rememberSaveable { mutableStateOf(false) }
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var trackForPlaylistDialog by remember { mutableStateOf<ArtistTrack?>(null) }
    var activeArtistSpotlight by remember { mutableStateOf<LocalArtistSpotlight?>(null) }
    var activePlaylistDetail by remember { mutableStateOf<Playlist?>(null) }

    if (currentTab != SonicTab.DISCOVER && !isFullPlayerOpen) {
        BackHandler {
            currentTab = SonicTab.DISCOVER
        }
    }

    val allTracks = (communityTracksState as? UiState.Success)?.data ?: LocalMusicCatalog.curatedTracks
    val myPlaylists = (myPlaylistsState as? UiState.Success)?.data ?: emptyList()
    val sharedPlaylists = (sharedPlaylistsState as? UiState.Success)?.data ?: LocalMusicCatalog.featuredCommunityPlaylists
    val savedTracks = (savedTracksState as? UiState.Success)?.data ?: emptyList()
    val savedTrackIds = remember(savedTracks) { savedTracks.map { it.trackId }.toSet() }

    if (isFullPlayerOpen && playerState.currentTrack != null) {
        val currentTrack = playerState.currentTrack!!
        FullVinylPlayerScreen(
            playerState = playerState,
            isSaved = currentTrack.id in savedTrackIds,
            onDismiss = { isFullPlayerOpen = false },
            onTogglePlay = viewModel::togglePlayPause,
            onSkipPrevious = viewModel::skipPrevious,
            onSkipNext = viewModel::skipNext,
            onSeek = viewModel::seekToFraction,
            onToggleShuffle = viewModel::toggleShuffle,
            onCycleRepeat = viewModel::cycleRepeatMode,
            onToggleSave = viewModel::toggleSaveTrack,
            onAddToPlaylist = { trackForPlaylistDialog = it },
            onShareTrack = { shareTrackIntent(context, it) },
            onSelectQueueTrack = { viewModel.playTrackOnline(it, playerState.queue) }
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = ObsidianBackground,
            floatingActionButton = {
                if (currentTab == SonicTab.PLAYLISTS) {
                    FloatingActionButton(
                        onClick = { showCreatePlaylistDialog = true },
                        containerColor = ElectricAmber,
                        contentColor = Color(0xFF1A1200),
                        modifier = Modifier.testTag("fab_create_playlist")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Create Curated Playlist")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Curate Mix", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            bottomBar = {
                Column {
                    if (playerState.currentTrack != null) {
                        MiniNowPlayingBar(
                            playerState = playerState,
                            onExpand = { isFullPlayerOpen = true },
                            onTogglePlay = viewModel::togglePlayPause,
                            onSkipNext = viewModel::skipNext
                        )
                    }
                    if (!isExpandedScreen) {
                        NavigationBar(
                            containerColor = ObsidianSurface
                        ) {
                            SonicTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SonicTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    AnimatedVisibility(visible = statusBanner != null) {
                        statusBanner?.let { msg ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clickable { viewModel.clearBanner() },
                                colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ElectricAmber,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = viewModel::clearBanner) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }

                    when (currentTab) {
                        SonicTab.DISCOVER -> DiscoverLocalScreen(
                            allTracks = allTracks,
                            selectedCity = selectedCity,
                            selectedGenre = selectedGenre,
                            searchQuery = searchQuery,
                            savedTrackIds = savedTrackIds,
                            currentlyPlayingTrackId = playerState.currentTrack?.id,
                            isPlaying = playerState.isPlaying,
                            onSelectCity = viewModel::selectCity,
                            onSelectGenre = viewModel::selectGenre,
                            onSearchChange = viewModel::updateSearchQuery,
                            onPlayTrack = { track, queue -> viewModel.playTrackOnline(track, queue) },
                            onToggleSaveTrack = viewModel::toggleSaveTrack,
                            onAddTrackToPlaylist = { trackForPlaylistDialog = it },
                            onOpenArtistSpotlight = { activeArtistSpotlight = it },
                            onShareTrack = { shareTrackIntent(context, it) }
                        )

                        SonicTab.PLAYLISTS -> FriendPlaylistsScreen(
                            myPlaylists = myPlaylists,
                            sharedPlaylists = sharedPlaylists,
                            discoveredPlaylist = discoveredSharePlaylist,
                            onClearDiscovered = viewModel::clearDiscoveredSharePlaylist,
                            onLookupShareCode = viewModel::lookupFriendPlaylistByCode,
                            onOpenCreatePlaylist = { showCreatePlaylistDialog = true },
                            onPlayPlaylist = viewModel::playPlaylistOnline,
                            onOpenPlaylistDetail = { activePlaylistDetail = it },
                            onLikePlaylist = viewModel::likeSharedPlaylist,
                            onSharePlaylist = { sharePlaylistIntent(context, it) }
                        )

                        SonicTab.PUBLISH -> PublishTrackOnlineScreen(
                            onPublishTrack = { artist, title, city, genre, url, bio, dur ->
                                viewModel.publishArtistTrackOnline(artist, title, city, genre, url, bio, dur)
                                currentTab = SonicTab.DISCOVER
                            }
                        )

                        SonicTab.CRATE -> MyCrateScreen(
                            userName = viewModel.currentUserName,
                            savedTracks = savedTracks,
                            myPlaylists = myPlaylists,
                            allTracks = allTracks,
                            onPlayTrack = { track -> viewModel.playTrackOnline(track, allTracks) },
                            onRemoveSaved = { trackId ->
                                allTracks.find { it.id == trackId }?.let { viewModel.toggleSaveTrack(it) }
                            },
                            onOpenPlaylist = { activePlaylistDetail = it },
                            onSignOut = {
                                signOutUser(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onSignOutComplete = onSignedOut,
                                    scope = scope
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            availableTracks = allTracks,
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = viewModel::createCuratedPlaylist
        )
    }

    trackForPlaylistDialog?.let { targetTrack ->
        AddTrackToPlaylistDialog(
            track = targetTrack,
            myPlaylists = myPlaylists,
            onDismiss = { trackForPlaylistDialog = null },
            onSelectPlaylist = { playlist -> viewModel.addTrackToPlaylist(playlist, targetTrack) },
            onOpenCreatePlaylist = { showCreatePlaylistDialog = true }
        )
    }

    activeArtistSpotlight?.let { spotlight ->
        ArtistSpotlightDetailDialog(
            spotlight = spotlight,
            onDismiss = { activeArtistSpotlight = null },
            onPlayAll = viewModel::playArtistSpotlight,
            onPlayTrack = { viewModel.playTrackOnline(it, spotlight.tracks) },
            onAddTrackToPlaylist = {
                activeArtistSpotlight = null
                trackForPlaylistDialog = it
            }
        )
    }

    activePlaylistDetail?.let { playlist ->
        PlaylistDetailSheetDialog(
            playlist = playlist,
            allTracks = allTracks,
            isOwner = playlist.ownerId == viewModel.currentUserId,
            onDismiss = { activePlaylistDetail = null },
            onPlayPlaylist = viewModel::playPlaylistOnline,
            onPlaySingleTrack = { track, queue -> viewModel.playTrackOnline(track, queue) },
            onSharePlaylist = { sharePlaylistIntent(context, it) },
            onToggleShareStatus = viewModel::toggleSharePlaylist,
            onRemoveTrack = viewModel::removeTrackFromPlaylist,
            onDeletePlaylist = viewModel::deleteMyPlaylist
        )
    }
}

@Composable
private fun DiscoverLocalScreen(
    allTracks: List<ArtistTrack>,
    selectedCity: String,
    selectedGenre: String,
    searchQuery: String,
    savedTrackIds: Set<String>,
    currentlyPlayingTrackId: String?,
    isPlaying: Boolean,
    onSelectCity: (String) -> Unit,
    onSelectGenre: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onPlayTrack: (ArtistTrack, List<ArtistTrack>) -> Unit,
    onToggleSaveTrack: (ArtistTrack) -> Unit,
    onAddTrackToPlaylist: (ArtistTrack) -> Unit,
    onOpenArtistSpotlight: (LocalArtistSpotlight) -> Unit,
    onShareTrack: (ArtistTrack) -> Unit
) {
    val filteredSpotlights = remember(selectedCity, selectedGenre, searchQuery) {
        LocalMusicCatalog.artistSpotlights.filter { artist ->
            val matchesCity = selectedCity == "All Cities" || artist.city == selectedCity
            val matchesGenre = selectedGenre == "All Scenes" || artist.primaryGenre == selectedGenre
            val matchesSearch = searchQuery.isBlank() ||
                artist.artistName.contains(searchQuery, ignoreCase = true) ||
                artist.neighborhood.contains(searchQuery, ignoreCase = true)
            matchesCity && matchesGenre && matchesSearch
        }
    }

    val filteredTracks = remember(allTracks, selectedCity, selectedGenre, searchQuery) {
        allTracks.filter { track ->
            val matchesCity = selectedCity == "All Cities" || track.city.equals(selectedCity, ignoreCase = true)
            val matchesGenre = selectedGenre == "All Scenes" || track.genre.equals(selectedGenre, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                track.trackTitle.contains(searchQuery, ignoreCase = true) ||
                track.artistName.contains(searchQuery, ignoreCase = true) ||
                track.city.contains(searchQuery, ignoreCase = true)
            matchesCity && matchesGenre && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("discover_screen_list"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Hero Stage Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF331B47),
                                    Color(0xFF1A1426),
                                    Color(0xFF3B1A22)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(ElectricAmber.copy(alpha = 0.22f), Color.Transparent),
                                center = Offset(size.width * 0.85f, size.height * 0.25f),
                                radius = size.minDimension * 0.7f
                            )
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = ElectricAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "LIVE ONLINE RADIO & LOCAL SCENE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricAmber,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Discover Your City's Underground Sound",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Stream original tracks from neighborhood bands, bedroom beatmakers, and vocalists—and share curated mixes with friends.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    if (filteredTracks.isNotEmpty()) {
                                        onPlayTrack(filteredTracks.first(), filteredTracks)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricAmber,
                                    contentColor = Color(0xFF1A1200)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("hero_stream_all_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stream Scene Online", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            // Search & Filter Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                placeholder = { Text("Search local artists, songs, or neighborhoods...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("discover_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // City Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LocalMusicCatalog.cities.forEach { city ->
                    FilterChip(
                        selected = selectedCity == city,
                        onClick = { onSelectCity(city) },
                        label = { Text(city) },
                        leadingIcon = if (selectedCity == city) {
                            { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricAmber.copy(alpha = 0.22f),
                            selectedLabelColor = ElectricAmber
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Genre Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LocalMusicCatalog.genres.forEach { genre ->
                    FilterChip(
                        selected = selectedGenre == genre,
                        onClick = { onSelectGenre(genre) },
                        label = { Text(genre) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VinylCoral.copy(alpha = 0.22f),
                            selectedLabelColor = VinylCoral
                        )
                    )
                }
            }
        }

        if (filteredSpotlights.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LOCAL ARTIST SPOTLIGHTS",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricAmber
                    )
                    Text(
                        text = "${filteredSpotlights.size} artists nearby",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredSpotlights, key = { it.id }) { spotlight ->
                        ArtistSpotlightCard(
                            spotlight = spotlight,
                            onClick = { onOpenArtistSpotlight(spotlight) },
                            onPlay = {
                                if (spotlight.tracks.isNotEmpty()) {
                                    onPlayTrack(spotlight.tracks.first(), spotlight.tracks)
                                }
                            }
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ONLINE TRACKS • STREAM INSTANTLY",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricAmber
                )
                Text(
                    text = "${filteredTracks.size} tracks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(filteredTracks, key = { it.id }) { track ->
            val isCurrent = track.id == currentlyPlayingTrackId
            val isSaved = track.id in savedTrackIds
            TrackListItemCard(
                track = track,
                isCurrent = isCurrent,
                isPlaying = isCurrent && isPlaying,
                isSaved = isSaved,
                onPlay = { onPlayTrack(track, filteredTracks) },
                onToggleSave = { onToggleSaveTrack(track) },
                onAddToPlaylist = { onAddTrackToPlaylist(track) },
                onShare = { onShareTrack(track) }
            )
        }
    }
}

@Composable
private fun ArtistSpotlightCard(
    spotlight: LocalArtistSpotlight,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    val accent = Color(spotlight.accentHex)
    Card(
        modifier = Modifier
            .width(260.dp)
            .clickable(onClick = onClick)
            .testTag("artist_spotlight_${spotlight.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accent.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = spotlight.primaryGenre,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Stream ${spotlight.artistName}",
                        tint = Color(0xFF14111D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = spotlight.artistName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${spotlight.neighborhood} • ${spotlight.city}",
                style = MaterialTheme.typography.bodyMedium,
                color = ElectricAmber
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = spotlight.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = spotlight.nextVenue,
                style = MaterialTheme.typography.labelSmall,
                color = StudioEmerald,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TrackListItemCard(
    track: ArtistTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isSaved: Boolean,
    onPlay: () -> Unit,
    onToggleSave: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onPlay)
            .testTag("track_card_${track.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) ObsidianCardElevated else ObsidianSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isCurrent) {
                            Brush.linearGradient(listOf(ElectricAmber, VinylCoral))
                        } else {
                            Brush.linearGradient(listOf(ObsidianCardElevated, Color(0xFF352B4D)))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                    contentDescription = "Play ${track.trackTitle}",
                    tint = if (isCurrent) ObsidianBackground else ElectricAmber
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.trackTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrent) ElectricAmber else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artistName} • ${track.city}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.genre} • ${formatDuration(track.durationSec.toInt())} • ${track.playsCount} online streams",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricAmber.copy(alpha = 0.85f)
                )
            }

            IconButton(
                onClick = onToggleSave,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("save_track_${track.id}")
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isSaved) "Remove from Saved" else "Save Track",
                    tint = if (isSaved) ElectricAmber else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onAddToPlaylist,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("add_to_playlist_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = "Add to Playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onShare,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Track",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FriendPlaylistsScreen(
    myPlaylists: List<Playlist>,
    sharedPlaylists: List<Playlist>,
    discoveredPlaylist: Playlist?,
    onClearDiscovered: () -> Unit,
    onLookupShareCode: (String) -> Unit,
    onOpenCreatePlaylist: () -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onOpenPlaylistDetail: (Playlist) -> Unit,
    onLikePlaylist: (Playlist) -> Unit,
    onSharePlaylist: (Playlist) -> Unit
) {
    var shareCodeInput by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("playlists_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Join / Discover Friend's Playlist by Invite Code
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "OPEN A FRIEND'S PLAYLIST BY CODE",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricAmber
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter a friend's invite code (try SONIC-NYC1 or SONIC-LA88) to stream their curated local mix online.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = shareCodeInput,
                            onValueChange = { shareCodeInput = it },
                            placeholder = { Text("SONIC-NYC1") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("friend_share_code_input")
                        )
                        Button(
                            onClick = { onLookupShareCode(shareCodeInput) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricAmber,
                                contentColor = Color(0xFF1A1200)
                            ),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("lookup_share_code_button")
                        ) {
                            Text("Find Mix", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (discoveredPlaylist != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        PlaylistFeatureCard(
                            playlist = discoveredPlaylist,
                            onPlay = { onPlayPlaylist(discoveredPlaylist) },
                            onOpenDetail = {
                                onOpenPlaylistDetail(discoveredPlaylist)
                                onClearDiscovered()
                            },
                            onLike = { onLikePlaylist(discoveredPlaylist) },
                            onShare = { onSharePlaylist(discoveredPlaylist) }
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YOUR CURATED PLAYLISTS (${myPlaylists.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricAmber
                )
                TextButton(
                    onClick = onOpenCreatePlaylist,
                    modifier = Modifier.testTag("header_new_playlist_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Playlist")
                }
            }
        }

        if (myPlaylists.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            tint = ElectricAmber,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Curate Your First Local Mix",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Bundle your favorite neighborhood artists and share an invite code with friends.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(myPlaylists, key = { it.id }) { playlist ->
                PlaylistFeatureCard(
                    playlist = playlist,
                    onPlay = { onPlayPlaylist(playlist) },
                    onOpenDetail = { onOpenPlaylistDetail(playlist) },
                    onLike = { onLikePlaylist(playlist) },
                    onShare = { onSharePlaylist(playlist) }
                )
            }
        }

        item {
            Text(
                text = "SHARED BY FRIENDS & LOCAL CURATORS",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricAmber
            )
        }

        items(sharedPlaylists, key = { "shared_${it.id}" }) { playlist ->
            PlaylistFeatureCard(
                playlist = playlist,
                onPlay = { onPlayPlaylist(playlist) },
                onOpenDetail = { onOpenPlaylistDetail(playlist) },
                onLike = { onLikePlaylist(playlist) },
                onShare = { onSharePlaylist(playlist) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
private fun PlaylistFeatureCard(
    playlist: Playlist,
    onPlay: () -> Unit,
    onOpenDetail: () -> Unit,
    onLike: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail)
            .testTag("playlist_card_${playlist.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(themeBrushForPlaylist(playlist.coverTheme)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = ObsidianBackground,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = ObsidianCardElevated,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = playlist.moodTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "• ${playlist.neighborhood}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = playlist.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "By ${playlist.ownerName} • ${playlist.trackIds.size} tracks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ElectricAmber)
                        .testTag("play_playlist_${playlist.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Playlist Online",
                        tint = Color(0xFF1A1200)
                    )
                }
            }

            if (playlist.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = playlist.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ObsidianCardElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Code: ${playlist.shareCode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioEmerald,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onLike,
                        modifier = Modifier.testTag("like_playlist_${playlist.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Like Playlist",
                            tint = VinylCoral,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${playlist.likesCount}", color = MaterialTheme.colorScheme.onSurface)
                    }

                    TextButton(
                        onClick = onShare,
                        modifier = Modifier.testTag("share_playlist_${playlist.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Playlist",
                            tint = ElectricAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", color = ElectricAmber)
                    }
                }
            }
        }
    }
}

@Composable
private fun PublishTrackOnlineScreen(
    onPublishTrack: (
        artistName: String,
        trackTitle: String,
        city: String,
        genre: String,
        streamUrl: String,
        bio: String,
        durationSec: Long
    ) -> Unit
) {
    var artistName by rememberSaveable { mutableStateOf("") }
    var trackTitle by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("Austin, TX") }
    var selectedGenre by rememberSaveable { mutableStateOf("Indie Rock") }
    var streamUrl by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }

    val availableGenres = LocalMusicCatalog.genres.filterNot { it == "All Scenes" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("publish_track_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "PUBLISH LOCAL MUSIC FOR ONLINE STREAMING",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricAmber
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Are you a local artist or band? Publish your track to the community catalog so anyone can discover and stream your music online.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = artistName,
                        onValueChange = { artistName = it },
                        label = { Text("Artist or Band Name") },
                        placeholder = { Text("e.g. Midnight Neon Transit") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publish_artist_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = trackTitle,
                        onValueChange = { trackTitle = it },
                        label = { Text("Track Title") },
                        placeholder = { Text("e.g. Congress Bridge Echoes") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publish_track_title_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City / Local Scene") },
                        placeholder = { Text("e.g. Austin, TX") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publish_city_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Select Genre (Configures Online Studio Synthesizer if no URL provided)",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricAmber
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableGenres.forEach { genre ->
                            FilterChip(
                                selected = selectedGenre == genre,
                                onClick = { selectedGenre = genre },
                                label = { Text(genre) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = streamUrl,
                        onValueChange = { streamUrl = it },
                        label = { Text("Online Audio Stream URL (Optional)") },
                        placeholder = { Text("https://... (Leave blank for Studio Synth Engine)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publish_stream_url_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Artist Story / Venue Notes") },
                        placeholder = { Text("Tell listeners where you record and perform locally...") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publish_bio_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onPublishTrack(
                                artistName,
                                trackTitle,
                                city,
                                selectedGenre,
                                streamUrl,
                                bio,
                                195L
                            )
                        },
                        enabled = artistName.isNotBlank() && trackTitle.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricAmber,
                            contentColor = Color(0xFF1A1200)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_publish_track_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Publish Track Online", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MyCrateScreen(
    userName: String,
    savedTracks: List<SavedTrack>,
    myPlaylists: List<Playlist>,
    allTracks: List<ArtistTrack>,
    onPlayTrack: (ArtistTrack) -> Unit,
    onRemoveSaved: (String) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onSignOut: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("my_crate_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LOCAL CURATOR PROFILE",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricAmber
                        )
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "${savedTracks.size} saved tracks • ${myPlaylists.size} curated mixes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out")
                    }
                }
            }
        }

        item {
            Text(
                text = "SAVED LOCAL TRACKS (${savedTracks.size})",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricAmber
            )
        }

        if (savedTracks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
                ) {
                    Text(
                        text = "Tap the bookmark icon on any local artist's track to save it to your personal crate.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(savedTracks, key = { it.id }) { saved ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val match = allTracks.find { it.id == saved.trackId } ?: ArtistTrack(
                                id = saved.trackId,
                                artistName = saved.artistName,
                                trackTitle = saved.trackTitle,
                                city = saved.city,
                                genre = saved.genre,
                                streamUrl = saved.streamUrl
                            )
                            onPlayTrack(match)
                        },
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = ElectricAmber
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = saved.trackTitle,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${saved.artistName} • ${saved.city}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onRemoveSaved(saved.trackId) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Remove from Crate",
                                tint = ElectricAmber
                            )
                        }
                    }
                }
            }
        }

        if (myPlaylists.isNotEmpty()) {
            item {
                Text(
                    text = "MY CURATED MIXES",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricAmber
                )
            }
            items(myPlaylists, key = { "crate_${it.id}" }) { playlist ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPlaylist(playlist) },
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.title,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${playlist.trackIds.size} tracks • Invite Code: ${playlist.shareCode}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StudioEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun sharePlaylistIntent(context: Context, playlist: Playlist) {
    val shareText = buildString {
        append("Listen to my curated local artist playlist \"${playlist.title}\" on SonicLocal!\n")
        append("Scene: ${playlist.neighborhood} (${playlist.moodTag})\n")
        append("Friend Invite Code: ${playlist.shareCode}\n")
        append("Open SonicLocal and enter code ${playlist.shareCode} to stream all ${playlist.trackIds.size} tracks online!")
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "SonicLocal Playlist: ${playlist.title}")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Playlist with Friends"))
}

private fun shareTrackIntent(context: Context, track: ArtistTrack) {
    val shareText = "Stream \"${track.trackTitle}\" by local artist ${track.artistName} (${track.city} • ${track.genre}) online on SonicLocal!"
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "${track.trackTitle} - ${track.artistName}")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Local Artist Track"))
}
