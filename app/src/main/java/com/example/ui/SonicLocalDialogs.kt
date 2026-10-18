package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ArtistTrack
import com.example.data.LocalArtistSpotlight
import com.example.data.Playlist
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.VinylCoral

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatePlaylistDialog(
    availableTracks: List<ArtistTrack>,
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        description: String,
        neighborhood: String,
        moodTag: String,
        coverTheme: String,
        isShared: Boolean,
        selectedTrackIds: List<String>
    ) -> Unit
) {
    BackHandler(onBack = onDismiss)

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("Silver Lake, LA") }
    var moodTag by remember { mutableStateOf("Late Night Indie") }
    var coverTheme by remember { mutableStateOf("amber") }
    var isShared by remember { mutableStateOf(true) }
    val selectedTracks = remember {
        mutableStateListOf<String>().apply {
            availableTracks.take(3).forEach { add(it.id) }
        }
    }

    val themes = listOf("amber", "crimson", "emerald", "violet", "cyan")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = "Curate Local Playlist",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Playlist Title") },
                        placeholder = { Text("e.g. Eastside Rooftop Grooves") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_playlist_title_input")
                    )
                }
                item {
                    OutlinedTextField(
                        value = neighborhood,
                        onValueChange = { neighborhood = it },
                        label = { Text("Local Scene / Neighborhood") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_playlist_neighborhood_input")
                    )
                }
                item {
                    OutlinedTextField(
                        value = moodTag,
                        onValueChange = { moodTag = it },
                        label = { Text("Mood or Vibe Tag") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Curator Notes for Friends") },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_playlist_description_input")
                    )
                }
                item {
                    Text(
                        text = "Cover Artwork Theme",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricAmber
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        themes.forEach { themeKey ->
                            FilterChip(
                                selected = coverTheme == themeKey,
                                onClick = { coverTheme = themeKey },
                                label = { Text(themeKey.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Share with Friends & Community",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Generates a shareable invite code so anyone can play online",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isShared,
                            onCheckedChange = { isShared = it },
                            modifier = Modifier.testTag("create_playlist_share_switch")
                        )
                    }
                }
                item {
                    Text(
                        text = "Select Tracks (${selectedTracks.size} chosen)",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricAmber
                    )
                }
                items(availableTracks, key = { it.id }) { track ->
                    val checked = track.id in selectedTracks
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (checked) selectedTracks.remove(track.id) else selectedTracks.add(track.id)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                if (isChecked) selectedTracks.add(track.id) else selectedTracks.remove(track.id)
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.trackTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${track.artistName} • ${track.city}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        title,
                        description,
                        neighborhood,
                        moodTag,
                        coverTheme,
                        isShared,
                        selectedTracks.toList()
                    )
                    onDismiss()
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricAmber,
                    contentColor = Color(0xFF1A1200)
                ),
                modifier = Modifier.testTag("confirm_create_playlist_button")
            ) {
                Text("Save Playlist", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddTrackToPlaylistDialog(
    track: ArtistTrack,
    myPlaylists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    onOpenCreatePlaylist: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = "Add \"${track.trackTitle}\" to Playlist",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            if (myPlaylists.isEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You haven't created any playlists yet. Create one now to start curating and sharing with friends!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(myPlaylists, key = { it.id }) { playlist ->
                        val alreadyIn = track.id in playlist.trackIds
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectPlaylist(playlist)
                                    onDismiss()
                                },
                            colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.title,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${playlist.trackIds.size} tracks • ${playlist.neighborhood}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = if (alreadyIn) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (alreadyIn) StudioEmerald else ElectricAmber
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onOpenCreatePlaylist()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricAmber,
                    contentColor = Color(0xFF1A1200)
                )
            ) {
                Text("New Playlist", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ArtistSpotlightDetailDialog(
    spotlight: LocalArtistSpotlight,
    onDismiss: () -> Unit,
    onPlayAll: (LocalArtistSpotlight) -> Unit,
    onPlayTrack: (ArtistTrack) -> Unit,
    onAddTrackToPlaylist: (ArtistTrack) -> Unit
) {
    BackHandler(onBack = onDismiss)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Column {
                Surface(
                    color = Color(spotlight.accentHex).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "${spotlight.primaryGenre} • ${spotlight.neighborhood}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(spotlight.accentHex),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = spotlight.artistName,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = spotlight.bio,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = VinylCoral
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Next Local Live Show",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricAmber
                            )
                            Text(
                                text = spotlight.nextVenue,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Text(
                    text = "Playable Online Tracks",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricAmber
                )

                spotlight.tracks.forEach { track ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayTrack(track) },
                        colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = ElectricAmber
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.trackTitle,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${formatDuration(track.durationSec.toInt())} • ${track.playsCount} streams",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onAddTrackToPlaylist(track) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add to Playlist",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPlayAll(spotlight)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricAmber,
                    contentColor = Color(0xFF1A1200)
                ),
                modifier = Modifier.testTag("play_artist_discography_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Stream Artist", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun PlaylistDetailSheetDialog(
    playlist: Playlist,
    allTracks: List<ArtistTrack>,
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onPlaySingleTrack: (ArtistTrack, List<ArtistTrack>) -> Unit,
    onSharePlaylist: (Playlist) -> Unit,
    onToggleShareStatus: (Playlist) -> Unit,
    onRemoveTrack: (Playlist, String) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit
) {
    BackHandler(onBack = onDismiss)

    val resolvedTracks = remember(playlist.trackIds, allTracks) {
        playlist.trackIds.mapNotNull { id -> allTracks.find { it.id == id } }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(themeBrushForPlaylist(playlist.coverTheme)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Curated by ${playlist.ownerName} • ${playlist.neighborhood}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricAmber
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    if (playlist.description.isNotBlank()) {
                        Text(
                            text = playlist.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
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
                                text = "Invite Code: ${playlist.shareCode}",
                                style = MaterialTheme.typography.labelLarge,
                                color = StudioEmerald,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        OutlinedButton(
                            onClick = { onSharePlaylist(playlist) },
                            modifier = Modifier.testTag("dialog_share_playlist_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share with Friends")
                        }
                    }

                    if (isOwner) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (playlist.isShared) "Publicly shared with friends" else "Private playlist",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row {
                                TextButton(onClick = { onToggleShareStatus(playlist) }) {
                                    Text(if (playlist.isShared) "Make Private" else "Share Online")
                                }
                                IconButton(
                                    onClick = {
                                        onDeletePlaylist(playlist)
                                        onDismiss()
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Playlist",
                                        tint = VinylCoral
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Tracks (${resolvedTracks.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricAmber
                    )
                }

                items(resolvedTracks, key = { it.id }) { track ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaySingleTrack(track, resolvedTracks) },
                        colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(ElectricAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Track",
                                    tint = ElectricAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.trackTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${track.artistName} • ${track.city}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isOwner) {
                                IconButton(
                                    onClick = { onRemoveTrack(playlist, track.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Track",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPlayPlaylist(playlist)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricAmber,
                    contentColor = Color(0xFF1A1200)
                ),
                modifier = Modifier.testTag("dialog_play_playlist_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play Full Mix Online", fontWeight = FontWeight.Bold)
            }
        }
    )
}
