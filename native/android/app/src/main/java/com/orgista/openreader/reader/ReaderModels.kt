package com.orgista.openreader.reader

import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator

enum class ReaderLayout {
    Page,
    Spread,
    Scroll;

    companion object {
        fun fromStored(value: String?): ReaderLayout = entries.firstOrNull { it.name == value } ?: Page
    }
}

data class ReaderHighlight(
    val id: String,
    val locator: Locator,
    val tint: Int,
)

data class FollowAlongWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

data class FollowAlongWordTrack(
    val paragraphs: List<List<FollowAlongWord>>,
)

object FollowAlongWordTrackCodec {
    fun decode(raw: String): FollowAlongWordTrack? = runCatching {
        val paragraphsJson = JSONObject(raw).getJSONArray("paragraphs")
        val paragraphs = (0 until paragraphsJson.length()).map { pi ->
            val wordsJson = paragraphsJson.getJSONArray(pi)
            (0 until wordsJson.length()).map { wi ->
                val word = wordsJson.getJSONObject(wi)
                FollowAlongWord(
                    text = word.getString("text"),
                    startMs = word.getLong("startMs"),
                    endMs = word.getLong("endMs"),
                )
            }
        }
        FollowAlongWordTrack(paragraphs)
    }.getOrNull()
}

/** Which paragraph, and which word inside it, the narration is on right now. */
data class FollowAlongCursor(
    val paragraphIndex: Int,
    val wordIndex: Int,
)

object FollowAlongWordTrackMapper {
    fun activeParagraphIndex(track: FollowAlongWordTrack, positionMs: Long): Int {
        val paragraphs = track.paragraphs
        if (paragraphs.isEmpty()) return 0
        val index = paragraphs.indexOfLast { paragraph -> (paragraph.firstOrNull()?.startMs ?: Long.MAX_VALUE) <= positionMs }
        return index.coerceIn(0, paragraphs.lastIndex)
    }

    /**
     * Resolves a playback position to a paragraph/word cursor. Kept allocation-light and free of
     * string building so it can run on every frame while audio plays — the rendering layer only
     * rebuilds surrounding paragraph text when [FollowAlongCursor.paragraphIndex] actually changes.
     */
    fun cursorAt(track: FollowAlongWordTrack, positionMs: Long): FollowAlongCursor {
        val paragraphs = track.paragraphs
        if (paragraphs.isEmpty()) return FollowAlongCursor(0, 0)
        val paragraphIndex = activeParagraphIndex(track, positionMs)
        val words = paragraphs[paragraphIndex]
        return FollowAlongCursor(
            paragraphIndex = paragraphIndex,
            wordIndex = words.indexOfLast { it.startMs <= positionMs }.coerceAtLeast(0),
        )
    }

    fun renderParagraph(words: List<FollowAlongWord>): String = words.joinToString(" ") { it.text }
}

data class FollowAlongPassages(
    val previous: String = "",
    val current: String = "Preparing the next passage…",
    val next: String = "",
)

object FollowAlongPassageMapper {
    fun fromLocators(locators: List<Locator>, activeIndex: Int): FollowAlongPassages = fromTexts(
        texts = locators.map(::passageText),
        activeIndex = activeIndex,
    )

    fun fromTexts(texts: List<String>, activeIndex: Int): FollowAlongPassages {
        if (texts.isEmpty()) return FollowAlongPassages()
        val safeIndex = activeIndex.coerceIn(texts.indices)
        val resolvedIndex = closestNonBlankIndex(texts, safeIndex) ?: return FollowAlongPassages()
        val current = compact(texts[resolvedIndex]).ifBlank { "Listening…" }
        return FollowAlongPassages(
            previous = nearestDistinct(texts, resolvedIndex, -1, current),
            current = current,
            next = nearestDistinct(texts, resolvedIndex, 1, current),
        )
    }

    private fun passageText(locator: Locator): String {
        val source = locator.text.highlight
            ?.takeIf(String::isNotBlank)
            ?: locator.text.after
                ?.takeIf(String::isNotBlank)
            ?: locator.text.before.orEmpty()
        return compact(source)
    }

    private fun nearestDistinct(
        texts: List<String>,
        activeIndex: Int,
        direction: Int,
        differentFrom: String = "",
    ): String {
        var index = activeIndex + direction
        while (index in texts.indices) {
            val candidate = compact(texts[index])
            if (candidate.isNotBlank() && candidate != differentFrom) return candidate
            index += direction
        }
        return ""
    }

    private fun closestNonBlankIndex(texts: List<String>, activeIndex: Int): Int? {
        if (compact(texts[activeIndex]).isNotBlank()) return activeIndex
        for (distance in 1..texts.lastIndex) {
            val after = activeIndex + distance
            if (after in texts.indices && compact(texts[after]).isNotBlank()) return after
            val before = activeIndex - distance
            if (before in texts.indices && compact(texts[before]).isNotBlank()) return before
        }
        return null
    }

    private fun compact(value: String): String {
        val normalized = value.replace(Regex("\\s+"), " ").trim()
        if (normalized.length <= MAX_PASSAGE_LENGTH) return normalized
        val breakAt = normalized.lastIndexOf(' ', MAX_PASSAGE_LENGTH)
            .takeIf { it >= MIN_PASSAGE_LENGTH }
            ?: MAX_PASSAGE_LENGTH
        return normalized.take(breakAt).trimEnd() + "…"
    }

    private const val MIN_PASSAGE_LENGTH = 96
    private const val MAX_PASSAGE_LENGTH = 210
}

object FollowAlongDocumentTextParser {
    fun parse(rawJavascriptResult: String?): List<String> {
        if (rawJavascriptResult.isNullOrBlank() || rawJavascriptResult == "null") return emptyList()
        return runCatching {
            val decoded = JSONObject("{\"value\":$rawJavascriptResult}").optString("value")
            JSONArray(decoded).let { array ->
                (0 until array.length())
                    .map { index -> array.optString(index).replace(Regex("\\s+"), " ").trim() }
                    .filter { it.length >= MIN_TEXT_LENGTH }
                    .distinct()
            }
        }.getOrDefault(emptyList())
    }

    private const val MIN_TEXT_LENGTH = 12
}

object ReaderHighlightCodec {
    fun encode(highlights: List<ReaderHighlight>): String = JSONArray().apply {
        highlights.forEach { highlight ->
            put(
                JSONObject()
                    .put("id", highlight.id)
                    .put("locator", highlight.locator.toJSON())
                    .put("tint", highlight.tint),
            )
        }
    }.toString()

    fun decode(raw: String?): List<ReaderHighlight> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optString("id").takeIf(String::isNotBlank) ?: return@mapNotNull null
                val locator = item.optJSONObject("locator")?.let(Locator::fromJSON) ?: return@mapNotNull null
                ReaderHighlight(id = id, locator = locator, tint = item.optInt("tint", DEFAULT_TINT))
            }
        }.getOrDefault(emptyList())
    }

    private const val DEFAULT_TINT = 0xFFF2C96D.toInt()
}
