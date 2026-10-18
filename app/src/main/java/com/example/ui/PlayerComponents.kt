package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.PlayerState
import com.example.audio.RepeatMode
import com.example.data.ArtistTrack
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.StageViolet
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.VinylCoral
import kotlin.math.cos
import kotlin.math.sin

fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return "%d:%02d".format(mins, secs)
}

@Composable
fun MiniNowPlayingBar(
    playerState: PlayerState,
    onExpand: () -> Unit,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack ?: return
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onExpand)
            .testTag("mini_player_bar"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Mini Equalizer Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ElectricAmber, VinylCoral)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (playerState.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = ObsidianBackground,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Now Playing",
                            tint = ObsidianBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.trackTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${track.artistName} • ${track.city}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricAmber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        tint = ElectricAmber,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            LinearProgressIndicator(
                progress = { playerState.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = ElectricAmber,
                trackColor = ObsidianSurface
            )
        }
    }
}

@Composable
fun FullVinylPlayerScreen(
    playerState: PlayerState,
    isSaved: Boolean,
    onDismiss: () -> Unit,
    onTogglePlay: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleSave: (ArtistTrack) -> Unit,
    onAddToPlaylist: (ArtistTrack) -> Unit,
    onShareTrack: (ArtistTrack) -> Unit,
    onSelectQueueTrack: (ArtistTrack) -> Unit
) {
    BackHandler(onBack = onDismiss)

    val track = playerState.currentTrack ?: return
    val infiniteTransition = rememberInfiniteTransition(label = "full_vinyl_rotation")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "spin_angle"
    )
    val effectiveAngle = if (playerState.isPlaying) spinAngle else 24f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF231734),
                        ObsidianBackground,
                        Color(0xFF08070C)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("full_vinyl_player_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("collapse_player_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Minimize Player",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ONLINE STREAMING DECK",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricAmber,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = playerState.streamModeLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onShareTrack(track) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("share_current_track_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Track",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Spinning Vinyl Record Artwork
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val r = size.minDimension / 2f

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(ElectricAmber.copy(alpha = 0.25f), Color.Transparent),
                                center = center,
                                radius = r * 1.1f
                            ),
                            radius = r * 1.1f
                        )

                        drawCircle(
                            color = Color(0xFF14111D),
                            radius = r * 0.94f,
                            center = center
                        )

                        for (groove in listOf(0.86f, 0.78f, 0.70f, 0.62f, 0.54f)) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                radius = r * groove,
                                center = center,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }

                        val rad = Math.toRadians(effectiveAngle.toDouble())
                        val arm = Offset(
                            x = center.x + (r * 0.80f * cos(rad)).toFloat(),
                            y = center.y + (r * 0.80f * sin(rad)).toFloat()
                        )
                        drawLine(
                            color = ElectricAmber.copy(alpha = 0.55f),
                            start = center,
                            end = arm,
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = listOf(ElectricAmber, VinylCoral, StageViolet)
                            ),
                            radius = r * 0.35f,
                            center = center
                        )
                        drawCircle(
                            color = ObsidianBackground,
                            radius = r * 0.09f,
                            center = center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Live 12-band Audio Spectrum Visualizer
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    val bands = playerState.spectrumBands
                    val count = bands.size
                    val spacing = 6.dp.toPx()
                    val barWidth = ((size.width - spacing * (count - 1)) / count).coerceAtLeast(4f)
                    bands.forEachIndexed { idx, level ->
                        val barHeight = (size.height * level.coerceIn(0.12f, 1f))
                        val x = idx * (barWidth + spacing)
                        val y = (size.height - barHeight) / 2f
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(ElectricAmber, VinylCoral)
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Track Title, Artist, City, Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.trackTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${track.artistName} • ${track.city}",
                            style = MaterialTheme.typography.titleMedium,
                            color = ElectricAmber
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = ObsidianSurface,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${track.genre} • ${track.playsCount} online plays",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { onToggleSave(track) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("full_player_save_button")
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isSaved) "Remove from Crate" else "Save to Crate",
                            tint = if (isSaved) ElectricAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { onAddToPlaylist(track) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("full_player_add_to_playlist_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = "Add to Playlist",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Seek Slider
                Slider(
                    value = playerState.progressFraction,
                    onValueChange = onSeek,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricAmber,
                        activeTrackColor = ElectricAmber,
                        inactiveTrackColor = ObsidianSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_seek_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(playerState.currentPositionSec),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration(playerState.durationSec),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Transport Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_shuffle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Toggle Shuffle",
                            tint = if (playerState.isShuffle) ElectricAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_previous_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(colors = listOf(ElectricAmber, VinylCoral))
                            )
                            .clickable(onClick = onTogglePlay)
                            .testTag("player_main_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = ObsidianBackground,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    IconButton(
                        onClick = onCycleRepeat,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_repeat_button")
                    ) {
                        Icon(
                            imageVector = if (playerState.repeatMode == RepeatMode.ONE) {
                                Icons.Default.RepeatOne
                            } else {
                                Icons.Default.Repeat
                            },
                            contentDescription = "Repeat Mode",
                            tint = if (playerState.repeatMode != RepeatMode.OFF) {
                                ElectricAmber
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Up Next Queue Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UP NEXT IN STREAM QUEUE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${playerState.queue.size} tracks",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricAmber
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(playerState.queue, key = { idx, item -> "${item.id}_$idx" }) { _, qTrack ->
                val isCurrent = qTrack.id == track.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectQueueTrack(qTrack) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) ObsidianCardElevated else ObsidianSurface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCurrent && playerState.isPlaying) {
                                Icons.Default.GraphicEq
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = null,
                            tint = if (isCurrent) ElectricAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = qTrack.trackTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isCurrent) ElectricAmber else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${qTrack.artistName} • ${qTrack.city}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = formatDuration(qTrack.durationSec.toInt()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

fun themeBrushForPlaylist(coverTheme: String): Brush {
    return when (coverTheme) {
        "crimson" -> Brush.linearGradient(listOf(VinylCoral, Color(0xFF7B1FA2)))
        "emerald" -> Brush.linearGradient(listOf(StudioEmerald, Color(0xFF00695C)))
        "violet" -> Brush.linearGradient(listOf(StageViolet, Color(0xFF283593)))
        "cyan" -> Brush.linearGradient(listOf(NeonCyan, Color(0xFF0277BD)))
        else -> Brush.linearGradient(listOf(ElectricAmber, VinylCoral))
    }
}
