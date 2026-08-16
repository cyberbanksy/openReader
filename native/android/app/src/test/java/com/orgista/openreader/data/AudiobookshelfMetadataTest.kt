package com.orgista.openreader.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudiobookshelfMetadataTest {
    @Test
    fun nullableStringTreatsJsonNullAndBlankValuesAsMissing() {
        assertNull(JSONObject("{\"description\":null}").nullableString("description"))
        assertNull(JSONObject("{\"description\":\"  \"}").nullableString("description"))
    }

    @Test
    fun nullableStringKeepsRealMetadata() {
        assertEquals(
            "A curious trip down a rabbit hole.",
            JSONObject("{\"description\":\"A curious trip down a rabbit hole.\"}")
                .nullableString("description"),
        )
    }
}
