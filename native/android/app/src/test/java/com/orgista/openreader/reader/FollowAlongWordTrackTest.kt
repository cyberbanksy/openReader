package com.orgista.openreader.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FollowAlongWordTrackTest {
    private val track = FollowAlongWordTrack(
        paragraphs = listOf(
            listOf(
                FollowAlongWord("Alice", 0L, 400L),
                FollowAlongWord("was", 400L, 700L),
                FollowAlongWord("beginning", 700L, 1_200L),
            ),
            listOf(
                FollowAlongWord("So", 2_000L, 2_300L),
                FollowAlongWord("she", 2_300L, 2_600L),
            ),
        ),
    )

    @Test
    fun cursorTracksTheWordCurrentlyBeingSpoken() {
        assertEquals(FollowAlongCursor(0, 0), FollowAlongWordTrackMapper.cursorAt(track, 0L))
        assertEquals(FollowAlongCursor(0, 1), FollowAlongWordTrackMapper.cursorAt(track, 450L))
        assertEquals(FollowAlongCursor(0, 2), FollowAlongWordTrackMapper.cursorAt(track, 1_100L))
        assertEquals(FollowAlongCursor(1, 1), FollowAlongWordTrackMapper.cursorAt(track, 2_400L))
    }

    @Test
    fun cursorHoldsTheOpeningWordBeforeNarrationReachesIt() {
        assertEquals(FollowAlongCursor(0, 0), FollowAlongWordTrackMapper.cursorAt(track, -500L))
    }

    @Test
    fun cursorHoldsTheFinalWordPastTheEndOfTheTrack() {
        assertEquals(FollowAlongCursor(1, 1), FollowAlongWordTrackMapper.cursorAt(track, 9_000L))
    }

    @Test
    fun gapBetweenParagraphsKeepsTheLastSpokenWord() {
        // 1.5s lands in the silence after paragraph one and before paragraph two starts.
        assertEquals(FollowAlongCursor(0, 2), FollowAlongWordTrackMapper.cursorAt(track, 1_500L))
    }

    @Test
    fun emptyTrackResolvesToTheOrigin() {
        val empty = FollowAlongWordTrack(paragraphs = emptyList())
        assertEquals(FollowAlongCursor(0, 0), FollowAlongWordTrackMapper.cursorAt(empty, 1_000L))
    }

    @Test
    fun paragraphRendersBackToPlainProse() {
        assertEquals("So she", FollowAlongWordTrackMapper.renderParagraph(track.paragraphs[1]))
    }

    @Test
    fun decodesRealTimingTrackJson() {
        val decoded = FollowAlongWordTrackCodec.decode(
            """
            {"baseOffsetMs":33000,"paragraphs":[[
              {"text":"Alice","startMs":0,"endMs":400},
              {"text":"was","startMs":400,"endMs":700}
            ]]}
            """.trimIndent(),
        )
        assertEquals(1, decoded?.paragraphs?.size)
        assertEquals("Alice was", decoded?.paragraphs?.first()?.let(FollowAlongWordTrackMapper::renderParagraph))
    }

    @Test
    fun malformedTimingTrackDecodesToNullRatherThanCrashingTheReader() {
        assertNull(FollowAlongWordTrackCodec.decode("not json"))
        assertNull(FollowAlongWordTrackCodec.decode("{}"))
    }
}
