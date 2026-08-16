package com.orgista.openreader.playback

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.orgista.openreader.data.AudiobookshelfApi
import com.orgista.openreader.data.SessionStore
import com.orgista.openreader.domain.BookFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class PlaybackServiceDeviceTest {
    @Test
    fun alicePlaybackCanPauseAndResume() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val session = requireNotNull(SessionStore(context).load()) {
            "The Lenovo must have its Audiobookshelf connection provisioned."
        }
        val api = AudiobookshelfApi()
        val audiobook = api.catalog(session).first {
            it.format == BookFormat.Audiobook && it.title.contains("Alice", ignoreCase = true)
        }
        val descriptor = api.startPlayback(session, audiobook)

        PlaybackService.play(context, descriptor)
        waitForPlayback { it.loaded && it.isPlaying }
        val startedAt = PlaybackService.state.value.totalPositionMs
        waitForPlayback { it.totalPositionMs > startedAt + 500L }

        PlaybackService.pause(context)
        waitForPlayback { !it.isPlaying }
        val pausedAt = PlaybackService.state.value.totalPositionMs
        delay(1_200L)
        val stillPausedAt = PlaybackService.state.value.totalPositionMs
        assertTrue(
            "Playback moved while paused: $pausedAt -> $stillPausedAt",
            abs(stillPausedAt - pausedAt) < 300L,
        )

        PlaybackService.toggle(context)
        waitForPlayback { it.isPlaying }
        waitForPlayback { it.totalPositionMs > pausedAt + 500L }

        PlaybackService.pause(context)
        waitForPlayback { !it.isPlaying }
    }

    private suspend fun waitForPlayback(predicate: (PlaybackUiState) -> Boolean) {
        withTimeout(20_000L) {
            while (!predicate(PlaybackService.state.value)) {
                delay(100L)
            }
        }
    }
}
