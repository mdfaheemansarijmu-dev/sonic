package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import com.example.data.ArtistTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatMode {
    OFF, ALL, ONE
}

data class PlayerState(
    val currentTrack: ArtistTrack? = null,
    val queue: List<ArtistTrack> = emptyList(),
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionSec: Int = 0,
    val durationSec: Int = 180,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val spectrumBands: List<Float> = List(12) { 0.15f },
    val streamModeLabel: String = "Ready to Stream Online"
) {
    val progressFraction: Float
        get() = if (durationSec > 0) (currentPositionSec.toFloat() / durationSec.toFloat()).coerceIn(0f, 1f) else 0f
}

class OnlineAudioPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var synthJob: Job? = null
    private var mediaProgressJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null

    @Volatile
    private var seekTargetSec: Int? = null

    fun playTrack(track: ArtistTrack, newQueue: List<ArtistTrack> = emptyList()) {
        val updatedQueue = if (newQueue.isNotEmpty()) {
            newQueue
        } else if (_state.value.queue.any { it.id == track.id }) {
            _state.value.queue
        } else {
            listOf(track) + _state.value.queue
        }

        stopCurrentEngines()

        val duration = track.durationSec.toInt().coerceAtLeast(30)
        _state.update {
            it.copy(
                currentTrack = track,
                queue = updatedQueue,
                isPlaying = true,
                isBuffering = true,
                currentPositionSec = 0,
                durationSec = duration,
                streamModeLabel = if (track.streamUrl.startsWith("http")) {
                    "Connecting to Online Stream..."
                } else {
                    "Streaming Online • Studio Direct (${track.genre})"
                }
            )
        }

        if (track.streamUrl.startsWith("http://") || track.streamUrl.startsWith("https://")) {
            startHttpStreamWithFallback(track)
        } else {
            startStudioSynthStream(track, startSec = 0)
        }
    }

    fun togglePlayPause() {
        val current = _state.value.currentTrack ?: return
        if (_state.value.isPlaying) {
            pause()
        } else {
            resumeOrRestart(current)
        }
    }

    fun pause() {
        _state.update { it.copy(isPlaying = false, spectrumBands = List(12) { 0.12f }) }
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {
        }
        synthJob?.cancel()
        synthJob = null
    }

    private fun resumeOrRestart(track: ArtistTrack) {
        _state.update { it.copy(isPlaying = true) }
        val mp = mediaPlayer
        if (mp != null) {
            try {
                mp.start()
                startMediaPlayerProgressLoop(mp)
                return
            } catch (_: Exception) {
                releaseMediaPlayer()
            }
        }
        startStudioSynthStream(track, startSec = _state.value.currentPositionSec)
    }

    fun seekToFraction(fraction: Float) {
        val targetSec = (fraction.coerceIn(0f, 1f) * _state.value.durationSec).toInt()
        _state.update { it.copy(currentPositionSec = targetSec) }
        val mp = mediaPlayer
        if (mp != null) {
            try {
                mp.seekTo(targetSec * 1000)
                return
            } catch (_: Exception) {
            }
        }
        seekTargetSec = targetSec
    }

    fun skipNext() {
        val currentState = _state.value
        val queue = currentState.queue
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.id == currentState.currentTrack?.id }
        val nextTrack = if (currentState.isShuffle && queue.size > 1) {
            queue.filter { it.id != currentState.currentTrack?.id }.random()
        } else if (currentIndex in 0 until queue.lastIndex) {
            queue[currentIndex + 1]
        } else {
            queue.first()
        }
        playTrack(nextTrack, queue)
    }

    fun skipPrevious() {
        val currentState = _state.value
        if (currentState.currentPositionSec > 4) {
            seekToFraction(0f)
            return
        }
        val queue = currentState.queue
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.id == currentState.currentTrack?.id }
        val prevTrack = if (currentIndex > 0) {
            queue[currentIndex - 1]
        } else {
            queue.last()
        }
        playTrack(prevTrack, queue)
    }

    fun toggleShuffle() {
        _state.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun cycleRepeatMode() {
        _state.update {
            val next = when (it.repeatMode) {
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
                RepeatMode.OFF -> RepeatMode.ALL
            }
            it.copy(repeatMode = next)
        }
    }

    private fun startHttpStreamWithFallback(track: ArtistTrack) {
        scope.launch(Dispatchers.IO) {
            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(track.streamUrl)
                    setOnPreparedListener { player ->
                        val durSec = (player.duration / 1000).takeIf { it > 0 } ?: track.durationSec.toInt()
                        _state.update {
                            it.copy(
                                isBuffering = false,
                                isPlaying = true,
                                durationSec = durSec,
                                streamModeLabel = "Live HTTP Audio Stream • Online"
                            )
                        }
                        player.start()
                        startMediaPlayerProgressLoop(player)
                    }
                    setOnCompletionListener {
                        onTrackEnded()
                    }
                    setOnErrorListener { _, _, _ ->
                        releaseMediaPlayer()
                        startStudioSynthStream(track, startSec = _state.value.currentPositionSec)
                        true
                    }
                }
                mediaPlayer = mp
                mp.prepareAsync()
            } catch (_: Exception) {
                releaseMediaPlayer()
                startStudioSynthStream(track, startSec = 0)
            }
        }
    }

    private fun startMediaPlayerProgressLoop(player: MediaPlayer) {
        mediaProgressJob?.cancel()
        mediaProgressJob = scope.launch {
            var tick = 0
            while (isActive && _state.value.isPlaying) {
                try {
                    if (player.isPlaying) {
                        val posSec = (player.currentPosition / 1000).coerceAtLeast(0)
                        val bands = List(12) { idx ->
                            val wave = abs(sin((tick * 0.35 + idx * 0.7))).toFloat()
                            (0.2f + 0.75f * wave).coerceIn(0.15f, 0.98f)
                        }
                        _state.update {
                            it.copy(
                                currentPositionSec = posSec,
                                isBuffering = false,
                                spectrumBands = bands
                            )
                        }
                    }
                } catch (_: Exception) {
                }
                tick++
                delay(120L)
            }
        }
    }

    private fun startStudioSynthStream(track: ArtistTrack, startSec: Int) {
        synthJob?.cancel()
        synthJob = scope.launch(Dispatchers.Default) {
            val sampleRate = 44100
            val chunkSamples = 2205 // 50ms per buffer
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(chunkSamples * 4)

            val audioTrack = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (_: Exception) {
                null
            }

            try {
                audioTrack?.play()
            } catch (_: Exception) {
            }

            _state.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = true,
                    streamModeLabel = "Online Studio Stream • 44.1kHz Stereo (${track.genre})"
                )
            }

            val profile = resolveSynthProfile(track)
            var currentSampleIndex = startSec.toLong() * sampleRate.toLong()
            val totalSamples = _state.value.durationSec.toLong() * sampleRate.toLong()
            val pcmBuffer = ShortArray(chunkSamples * 2)
            var uiCounter = 0

            try {
                while (isActive && _state.value.isPlaying && currentSampleIndex < totalSamples) {
                    val requestedSeek = seekTargetSec
                    if (requestedSeek != null) {
                        currentSampleIndex = requestedSeek.toLong() * sampleRate.toLong()
                        seekTargetSec = null
                    }

                    val secPerBeat = 60.0 / profile.bpm
                    var peakEnergy = 0.0

                    for (i in 0 until chunkSamples) {
                        val samplePos = currentSampleIndex + i
                        val t = samplePos.toDouble() / sampleRate.toDouble()
                        val beatPos = t / secPerBeat
                        val barIndex = (beatPos / 4.0).toInt()
                        val chord = profile.chords[barIndex % profile.chords.size]

                        // Warm polyphonic chord pad
                        val pad1 = sin(2.0 * PI * chord[0] * t)
                        val pad2 = sin(2.0 * PI * chord[1] * t * 1.001)
                        val pad3 = sin(2.0 * PI * chord[2] * t * 0.999)
                        val padSignal = (pad1 + pad2 + pad3) * 0.14

                        // Arpeggio / lead melody note
                        val stepIndex = (beatPos * 2.0).toInt()
                        val stepFrac = (beatPos * 2.0) - stepIndex
                        val leadFreq = chord[stepIndex % chord.size] * profile.leadOctaveMultiplier
                        val leadEnv = exp(-stepFrac * profile.leadDecay)
                        val leadSignal = sin(2.0 * PI * leadFreq * t + 0.4 * sin(2.0 * PI * leadFreq * 2.0 * t)) * leadEnv * 0.20

                        // Sub bassline
                        val bassFreq = chord[0] * 0.5
                        val beatFrac = beatPos - beatPos.toInt()
                        val bassEnv = exp(-beatFrac * 2.8)
                        val bassSignal = sin(2.0 * PI * bassFreq * t) * bassEnv * 0.26

                        // Soft kick & snare pulse
                        val kickEnv = if (beatPos.toInt() % 2 == 0) exp(-beatFrac * 14.0) else 0.0
                        val kickFreq = 52.0 + 95.0 * exp(-beatFrac * 28.0)
                        val kickSignal = sin(2.0 * PI * kickFreq * t) * kickEnv * 0.25

                        val leftMix = (padSignal + leadSignal * 0.9 + bassSignal + kickSignal).coerceIn(-0.95, 0.95)
                        val rightMix = (padSignal * 0.95 + leadSignal * 1.1 + bassSignal + kickSignal).coerceIn(-0.95, 0.95)

                        pcmBuffer[i * 2] = (leftMix * Short.MAX_VALUE).toInt().toShort()
                        pcmBuffer[i * 2 + 1] = (rightMix * Short.MAX_VALUE).toInt().toShort()
                        peakEnergy += abs(leftMix)
                    }

                    if (audioTrack != null) {
                        audioTrack.write(pcmBuffer, 0, pcmBuffer.size)
                    } else {
                        delay(50L)
                    }

                    currentSampleIndex += chunkSamples
                    uiCounter++
                    if (uiCounter % 3 == 0) {
                        val posSec = (currentSampleIndex / sampleRate).toInt()
                        val avgEnergy = (peakEnergy / chunkSamples).toFloat().coerceIn(0.1f, 0.9f)
                        val bands = List(12) { bandIdx ->
                            val phase = sin(uiCounter * 0.45 + bandIdx * 0.65).toFloat()
                            (0.22f + avgEnergy * 0.65f + 0.2f * abs(phase)).coerceIn(0.12f, 0.98f)
                        }
                        _state.update {
                            it.copy(
                                currentPositionSec = posSec,
                                spectrumBands = bands
                            )
                        }
                    }
                }

                if (isActive && _state.value.isPlaying && currentSampleIndex >= totalSamples) {
                    onTrackEnded()
                }
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun onTrackEnded() {
        val currentState = _state.value
        val current = currentState.currentTrack ?: return
        when (currentState.repeatMode) {
            RepeatMode.ONE -> playTrack(current, currentState.queue)
            RepeatMode.ALL -> skipNext()
            RepeatMode.OFF -> {
                val idx = currentState.queue.indexOfFirst { it.id == current.id }
                if (idx in 0 until currentState.queue.lastIndex) {
                    skipNext()
                } else {
                    _state.update { it.copy(isPlaying = false, currentPositionSec = 0) }
                }
            }
        }
    }

    private fun stopCurrentEngines() {
        synthJob?.cancel()
        synthJob = null
        mediaProgressJob?.cancel()
        mediaProgressJob = null
        releaseMediaPlayer()
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
    }

    fun release() {
        stopCurrentEngines()
    }

    private data class SynthProfile(
        val bpm: Double,
        val chords: List<DoubleArray>,
        val leadOctaveMultiplier: Double,
        val leadDecay: Double
    )

    private fun resolveSynthProfile(track: ArtistTrack): SynthProfile {
        val seed = abs(track.id.hashCode() % 3)
        return when (track.genre) {
            "Neo-Soul" -> SynthProfile(
                bpm = 88.0 + seed * 4,
                chords = listOf(
                    doubleArrayOf(261.63, 311.13, 392.00, 466.16), // Cm7
                    doubleArrayOf(349.23, 415.30, 523.25, 622.25), // Fm7
                    doubleArrayOf(233.08, 293.66, 349.23, 440.00), // Bbmaj7
                    doubleArrayOf(311.13, 392.00, 466.16, 587.33)  // Ebmaj7
                ),
                leadOctaveMultiplier = 2.0,
                leadDecay = 3.2
            )
            "Lo-Fi Beats" -> SynthProfile(
                bpm = 76.0 + seed * 3,
                chords = listOf(
                    doubleArrayOf(293.66, 349.23, 440.00, 523.25), // Dm7
                    doubleArrayOf(392.00, 493.88, 587.33, 698.46), // G7
                    doubleArrayOf(261.63, 329.63, 392.00, 493.88), // Cmaj7
                    doubleArrayOf(220.00, 261.63, 329.63, 392.00)  // Am7
                ),
                leadOctaveMultiplier = 1.5,
                leadDecay = 4.0
            )
            "Synthwave" -> SynthProfile(
                bpm = 118.0 + seed * 4,
                chords = listOf(
                    doubleArrayOf(220.00, 261.63, 329.63), // Am
                    doubleArrayOf(174.61, 220.00, 261.63), // F
                    doubleArrayOf(261.63, 329.63, 392.00), // C
                    doubleArrayOf(196.00, 246.94, 293.66)  // G
                ),
                leadOctaveMultiplier = 2.0,
                leadDecay = 5.0
            )
            "Folk Acoustic" -> SynthProfile(
                bpm = 96.0 + seed * 3,
                chords = listOf(
                    doubleArrayOf(196.00, 246.94, 293.66), // G
                    doubleArrayOf(293.66, 369.99, 440.00), // D
                    doubleArrayOf(164.81, 196.00, 246.94), // Em
                    doubleArrayOf(261.63, 329.63, 392.00)  // C
                ),
                leadOctaveMultiplier = 2.0,
                leadDecay = 3.8
            )
            "Underground Hip-Hop" -> SynthProfile(
                bpm = 92.0 + seed * 2,
                chords = listOf(
                    doubleArrayOf(246.94, 293.66, 369.99, 440.00), // Bm7
                    doubleArrayOf(329.63, 392.00, 493.88, 587.33), // Em7
                    doubleArrayOf(220.00, 277.18, 329.63, 415.30), // Amaj7
                    doubleArrayOf(277.18, 329.63, 415.30, 493.88)  // C#m7
                ),
                leadOctaveMultiplier = 1.5,
                leadDecay = 3.5
            )
            else -> SynthProfile( // Indie Rock
                bpm = 112.0 + seed * 3,
                chords = listOf(
                    doubleArrayOf(261.63, 329.63, 392.00), // C
                    doubleArrayOf(220.00, 261.63, 329.63), // Am
                    doubleArrayOf(174.61, 220.00, 261.63), // F
                    doubleArrayOf(196.00, 246.94, 293.66)  // G
                ),
                leadOctaveMultiplier = 2.0,
                leadDecay = 4.2
            )
        }
    }
}
