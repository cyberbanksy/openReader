package com.orgista.openreader.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderModelsTest {
    @Test
    fun unknownLayoutFallsBackToCenteredPage() {
        assertEquals(ReaderLayout.Page, ReaderLayout.fromStored("something-new"))
    }
}
