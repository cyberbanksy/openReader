package com.orgista.openreader.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackTimelineTest {
    @Test
    fun resolvesSavedBookPositionIntoTrackAndOffset() {
        assertEquals(1 to 15_000L, PlaybackTimeline.resolvePosition(doubleArrayOf(30.0, 40.0), 45_000L))
    }

    @Test
    fun reportsProgressAcrossEveryTrack() {
        val durations = doubleArrayOf(30.0, 40.0, 30.0)

        assertEquals(50_000L, PlaybackTimeline.totalPositionMs(durations, trackIndex = 1, positionMs = 20_000L))
        assertEquals(0.5f, PlaybackTimeline.progress(durations, trackIndex = 1, positionMs = 20_000L), 0.0001f)
    }

    @Test
    fun clampsPositionsAtBookBounds() {
        assertEquals(1 to 40_000L, PlaybackTimeline.resolvePosition(doubleArrayOf(30.0, 40.0), 99_000L))
        assertEquals(1f, PlaybackTimeline.progress(doubleArrayOf(30.0, 40.0), 1, 99_000L), 0.0001f)
    }
}
