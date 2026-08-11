package com.orgista.openreader.playback

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.orgista.openreader.data.PlaybackDescriptor

class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PLAY) {
            val urls = intent.getStringArrayListExtra(EXTRA_URLS).orEmpty()
            val titles = intent.getStringArrayListExtra(EXTRA_TRACK_TITLES).orEmpty()
            val bookTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()
            val creator = intent.getStringExtra(EXTRA_CREATOR).orEmpty()
            val coverUrl = intent.getStringExtra(EXTRA_COVER_URL)
            val durationSeconds = intent.getDoubleArrayExtra(EXTRA_DURATIONS) ?: doubleArrayOf()
            val currentTimeMs = intent.getLongExtra(EXTRA_CURRENT_TIME_MS, 0L)
            val mediaItems = urls.mapIndexed { index, url ->
                MediaItem.Builder()
                    .setMediaId("${intent.getStringExtra(EXTRA_SESSION_ID)}-$index")
                    .setUri(url)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(bookTitle)
                            .setArtist(creator)
                            .setAlbumTitle(titles.getOrNull(index) ?: bookTitle)
                            .setArtworkUri(coverUrl?.let(android.net.Uri::parse))
                            .build(),
                    )
                    .build()
            }
            if (mediaItems.isNotEmpty()) {
                val (mediaIndex, positionMs) = resolvePosition(durationSeconds, currentTimeMs)
                player.setMediaItems(mediaItems, mediaIndex, positionMs)
                player.prepare()
                player.playWhenReady = true
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        player.release()
        super.onDestroy()
    }

    private fun resolvePosition(durations: DoubleArray, totalPositionMs: Long): Pair<Int, Long> {
        var remaining = totalPositionMs.coerceAtLeast(0L)
        durations.forEachIndexed { index, duration ->
            val durationMs = (duration * 1_000L).toLong()
            if (remaining < durationMs || index == durations.lastIndex) return index to remaining
            remaining -= durationMs
        }
        return 0 to totalPositionMs.coerceAtLeast(0L)
    }

    companion object {
        private const val ACTION_PLAY = "com.orgista.openreader.action.PLAY"
        private const val EXTRA_SESSION_ID = "session_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_CREATOR = "creator"
        private const val EXTRA_COVER_URL = "cover_url"
        private const val EXTRA_URLS = "urls"
        private const val EXTRA_TRACK_TITLES = "track_titles"
        private const val EXTRA_DURATIONS = "durations"
        private const val EXTRA_CURRENT_TIME_MS = "current_time_ms"

        fun play(context: Context, descriptor: PlaybackDescriptor) {
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_SESSION_ID, descriptor.sessionId)
                putExtra(EXTRA_TITLE, descriptor.title)
                putExtra(EXTRA_CREATOR, descriptor.creator)
                putExtra(EXTRA_COVER_URL, descriptor.coverUrl)
                putStringArrayListExtra(EXTRA_URLS, ArrayList(descriptor.tracks.map { it.url }))
                putStringArrayListExtra(EXTRA_TRACK_TITLES, ArrayList(descriptor.tracks.map { it.title }))
                putExtra(EXTRA_DURATIONS, descriptor.tracks.map { it.durationSeconds }.toDoubleArray())
                putExtra(EXTRA_CURRENT_TIME_MS, (descriptor.currentTimeSeconds * 1_000L).toLong())
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
