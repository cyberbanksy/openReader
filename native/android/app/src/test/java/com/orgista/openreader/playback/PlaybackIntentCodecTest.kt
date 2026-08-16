package com.orgista.openreader.playback

import com.orgista.openreader.data.PlaybackDescriptor
import com.orgista.openreader.data.PlaybackTrack
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackIntentCodecTest {
    @Test
    fun playbackDescriptorRoundTripsThroughAStringExtra() {
        val descriptor = PlaybackDescriptor(
            sessionId = "session-1",
            title = "Alice's Adventures in Wonderland",
            creator = "Lewis Carroll",
            coverUrl = "http://server/cover",
            currentTimeSeconds = 42.5,
            tracks = listOf(
                PlaybackTrack(1, "Part 1", "http://server/track/1", 20.0),
                PlaybackTrack(2, "Part 2", "http://server/track/2", 30.0),
            ),
        )

        val restored = PlaybackIntentCodec.decode(PlaybackIntentCodec.encode(descriptor))

        assertEquals(descriptor.sessionId, restored.sessionId)
        assertEquals(descriptor.title, restored.title)
        assertEquals(descriptor.creator, restored.creator)
        assertEquals(descriptor.coverUrl, restored.coverUrl)
        assertEquals(42_500L, restored.currentTimeMs)
        assertEquals(listOf("http://server/track/1", "http://server/track/2"), restored.urls)
        assertEquals(listOf("Part 1", "Part 2"), restored.trackTitles)
        assertArrayEquals(doubleArrayOf(20.0, 30.0), restored.durationsSeconds, 0.0)
    }
}
