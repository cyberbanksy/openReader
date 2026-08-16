package com.orgista.openreader.playback

import com.orgista.openreader.data.PlaybackDescriptor
import org.json.JSONArray
import org.json.JSONObject

data class PlaybackIntentData(
    val sessionId: String,
    val title: String,
    val creator: String,
    val coverUrl: String?,
    val currentTimeMs: Long,
    val urls: ArrayList<String>,
    val trackTitles: ArrayList<String>,
    val durationsSeconds: DoubleArray,
)

object PlaybackIntentCodec {
    fun encode(descriptor: PlaybackDescriptor): String = JSONObject()
        .put("sessionId", descriptor.sessionId)
        .put("title", descriptor.title)
        .put("creator", descriptor.creator)
        .put("coverUrl", descriptor.coverUrl)
        .put("currentTimeMs", (descriptor.currentTimeSeconds * 1_000L).toLong())
        .put(
            "tracks",
            JSONArray().apply {
                descriptor.tracks.forEach { track ->
                    put(
                        JSONObject()
                            .put("url", track.url)
                            .put("title", track.title)
                            .put("durationSeconds", track.durationSeconds),
                    )
                }
            },
        )
        .toString()

    fun decode(raw: String): PlaybackIntentData {
        val json = JSONObject(raw)
        val tracks = json.getJSONArray("tracks")
        val urls = ArrayList<String>(tracks.length())
        val titles = ArrayList<String>(tracks.length())
        val durations = DoubleArray(tracks.length())
        repeat(tracks.length()) { index ->
            val track = tracks.getJSONObject(index)
            urls += track.getString("url")
            titles += track.optString("title")
            durations[index] = track.optDouble("durationSeconds", 0.0)
        }
        return PlaybackIntentData(
            sessionId = json.getString("sessionId"),
            title = json.optString("title"),
            creator = json.optString("creator"),
            coverUrl = json.optString("coverUrl").takeIf(String::isNotBlank),
            currentTimeMs = json.optLong("currentTimeMs", 0L),
            urls = urls,
            trackTitles = titles,
            durationsSeconds = durations,
        )
    }
}
