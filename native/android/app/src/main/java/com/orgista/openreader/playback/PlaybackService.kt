package com.orgista.openreader.playback

import android.content.Context
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.orgista.openreader.data.PlaybackDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val loaded: Boolean = false,
    val title: String = "",
    val creator: String = "",
    val trackTitle: String = "",
    val isPlaying: Boolean = false,
    val trackIndex: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val totalPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
) {
    val progress: Float
        get() = if (totalDurationMs <= 0L) 0f else {
            (totalPositionMs.toDouble() / totalDurationMs).toFloat().coerceIn(0f, 1f)
        }
}

class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var bookTitle = ""
    private var creator = ""
    private var trackTitles = emptyList<String>()
    private var durationsSeconds = doubleArrayOf()

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                publishState()
            }
        })
        mediaSession = MediaSession.Builder(this, player).build()
        serviceScope.launch {
            while (isActive) {
                publishState()
                delay(500)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val descriptor = intent.getStringExtra(EXTRA_DESCRIPTOR)
                    ?.let(PlaybackIntentCodec::decode)
                val urls = descriptor?.urls ?: intent.getStringArrayListExtra(EXTRA_URLS).orEmpty()
                trackTitles = descriptor?.trackTitles
                    ?: intent.getStringArrayListExtra(EXTRA_TRACK_TITLES).orEmpty()
                bookTitle = descriptor?.title ?: intent.getStringExtra(EXTRA_TITLE).orEmpty()
                creator = descriptor?.creator ?: intent.getStringExtra(EXTRA_CREATOR).orEmpty()
                val coverUrl = descriptor?.coverUrl ?: intent.getStringExtra(EXTRA_COVER_URL)
                durationsSeconds = descriptor?.durationsSeconds
                    ?: intent.getDoubleArrayExtra(EXTRA_DURATIONS)
                    ?: doubleArrayOf()
                val currentTimeMs = descriptor?.currentTimeMs
                    ?: intent.getLongExtra(EXTRA_CURRENT_TIME_MS, 0L)
                val mediaItems = urls.mapIndexed { index, url ->
                    MediaItem.Builder()
                        .setMediaId("${intent.getStringExtra(EXTRA_SESSION_ID)}-$index")
                        .setUri(url)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(bookTitle)
                                .setArtist(creator)
                                .setAlbumTitle(trackTitles.getOrNull(index) ?: bookTitle)
                                .setArtworkUri(coverUrl?.let(android.net.Uri::parse))
                                .build(),
                        )
                        .build()
                }
                if (mediaItems.isNotEmpty()) {
                    val (mediaIndex, positionMs) = PlaybackTimeline.resolvePosition(durationsSeconds, currentTimeMs)
                    player.setMediaItems(mediaItems, mediaIndex, positionMs)
                    player.prepare()
                    player.playWhenReady = true
                }
            }
            ACTION_TOGGLE -> if (player.playWhenReady) player.pause() else player.play()
            ACTION_PAUSE -> player.pause()
            ACTION_SEEK_BY -> player.seekTo((player.currentPosition + intent.getLongExtra(EXTRA_SEEK_MS, 0L)).coerceAtLeast(0L))
            ACTION_SEEK_TO_BOOK -> seekToBookPosition(intent.getLongExtra(EXTRA_SEEK_MS, 0L))
            ACTION_SKIP_NEXT -> player.seekToNextMediaItem()
            ACTION_SKIP_PREVIOUS -> player.seekToPreviousMediaItem()
        }
        publishState()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.release()
        mediaSession = null
        player.release()
        mutableState.value = PlaybackUiState()
        super.onDestroy()
    }

    private fun seekToBookPosition(totalPositionMs: Long) {
        if (player.mediaItemCount == 0) return
        val (index, offset) = PlaybackTimeline.resolvePosition(durationsSeconds, totalPositionMs)
        player.seekTo(index.coerceIn(0, player.mediaItemCount - 1), offset)
    }

    private fun publishState() {
        if (!::player.isInitialized || player.mediaItemCount == 0) return
        val trackIndex = player.currentMediaItemIndex.coerceAtLeast(0)
        val totalPosition = PlaybackTimeline.totalPositionMs(durationsSeconds, trackIndex, player.currentPosition)
        mutableState.value = PlaybackUiState(
            loaded = true,
            title = bookTitle,
            creator = creator,
            trackTitle = trackTitles.getOrNull(trackIndex).orEmpty().ifBlank { "Part ${trackIndex + 1}" },
            isPlaying = player.playWhenReady && player.playbackState != Player.STATE_ENDED,
            trackIndex = trackIndex,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.coerceAtLeast(0L),
            totalPositionMs = totalPosition,
            totalDurationMs = PlaybackTimeline.totalDurationMs(durationsSeconds),
        )
    }

    companion object {
        private const val ACTION_PLAY = "com.orgista.openreader.action.PLAY"
        private const val ACTION_TOGGLE = "com.orgista.openreader.action.TOGGLE"
        private const val ACTION_PAUSE = "com.orgista.openreader.action.PAUSE"
        private const val ACTION_SEEK_BY = "com.orgista.openreader.action.SEEK_BY"
        private const val ACTION_SEEK_TO_BOOK = "com.orgista.openreader.action.SEEK_TO_BOOK"
        private const val ACTION_SKIP_NEXT = "com.orgista.openreader.action.SKIP_NEXT"
        private const val ACTION_SKIP_PREVIOUS = "com.orgista.openreader.action.SKIP_PREVIOUS"
        private const val EXTRA_SESSION_ID = "session_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_CREATOR = "creator"
        private const val EXTRA_COVER_URL = "cover_url"
        private const val EXTRA_URLS = "urls"
        private const val EXTRA_TRACK_TITLES = "track_titles"
        private const val EXTRA_DURATIONS = "durations"
        private const val EXTRA_CURRENT_TIME_MS = "current_time_ms"
        private const val EXTRA_DESCRIPTOR = "playback_descriptor"
        private const val EXTRA_SEEK_MS = "seek_ms"
        private val mutableState = MutableStateFlow(PlaybackUiState())
        val state: StateFlow<PlaybackUiState> = mutableState.asStateFlow()

        fun play(context: Context, descriptor: PlaybackDescriptor) {
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_DESCRIPTOR, PlaybackIntentCodec.encode(descriptor))
            }
            // MediaSessionService owns its playback notification and promotes itself once
            // the player becomes active. Starting it as an FGS here races that promotion
            // and Android 14 kills the process before Media3 can call startForeground().
            context.startService(intent)
        }

        fun toggle(context: Context) = send(context, ACTION_TOGGLE)

        fun pause(context: Context) = send(context, ACTION_PAUSE)

        fun seekBy(context: Context, offsetMs: Long) = send(context, ACTION_SEEK_BY, offsetMs)

        fun seekTo(context: Context, totalPositionMs: Long) = send(context, ACTION_SEEK_TO_BOOK, totalPositionMs)

        fun next(context: Context) = send(context, ACTION_SKIP_NEXT)

        fun previous(context: Context) = send(context, ACTION_SKIP_PREVIOUS)

        private fun send(context: Context, action: String, seekMs: Long? = null) {
            context.startService(
                Intent(context, PlaybackService::class.java).apply {
                    this.action = action
                    seekMs?.let { putExtra(EXTRA_SEEK_MS, it) }
                },
            )
        }
    }
}
