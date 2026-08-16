package com.orgista.openreader.playback

import kotlin.math.roundToLong

object PlaybackTimeline {
    fun resolvePosition(durationsSeconds: DoubleArray, totalPositionMs: Long): Pair<Int, Long> {
        if (durationsSeconds.isEmpty()) return 0 to totalPositionMs.coerceAtLeast(0L)
        var remaining = totalPositionMs.coerceAtLeast(0L)
        durationsSeconds.forEachIndexed { index, durationSeconds ->
            val durationMs = durationMs(durationSeconds)
            if (remaining < durationMs || index == durationsSeconds.lastIndex) {
                return index to remaining.coerceIn(0L, durationMs)
            }
            remaining -= durationMs
        }
        return 0 to 0L
    }

    fun totalPositionMs(durationsSeconds: DoubleArray, trackIndex: Int, positionMs: Long): Long {
        val before = durationsSeconds.take(trackIndex.coerceAtLeast(0))
            .sumOf(::durationMs)
        val currentDuration = durationsSeconds.getOrNull(trackIndex)?.let(::durationMs)
        return before + if (currentDuration == null) {
            positionMs.coerceAtLeast(0L)
        } else {
            positionMs.coerceIn(0L, currentDuration)
        }
    }

    fun totalDurationMs(durationsSeconds: DoubleArray): Long = durationsSeconds.sumOf(::durationMs)

    fun progress(durationsSeconds: DoubleArray, trackIndex: Int, positionMs: Long): Float {
        val total = totalDurationMs(durationsSeconds)
        if (total <= 0L) return 0f
        return (totalPositionMs(durationsSeconds, trackIndex, positionMs).toDouble() / total)
            .toFloat()
            .coerceIn(0f, 1f)
    }

    private fun durationMs(seconds: Double): Long = (seconds.coerceAtLeast(0.0) * 1_000.0).roundToLong()
}
