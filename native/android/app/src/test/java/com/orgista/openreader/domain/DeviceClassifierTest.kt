package com.orgista.openreader.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceClassifierTest {
    @Test
    fun compactWindowIsPhone() {
        val profile = DeviceClassifier.classify(
            widthClassDp = 0,
            smallestWidthDp = 411,
            hasHinge = false,
            platformMode = PlatformMode.Normal,
        )

        assertEquals(DeviceCategory.Phone, profile.category)
        assertEquals(NavigationStyle.BottomBar, profile.navigationStyle)
    }

    @Test
    fun expandedWindowOnLargeDisplayIsTablet() {
        val profile = DeviceClassifier.classify(
            widthClassDp = 840,
            smallestWidthDp = 800,
            hasHinge = false,
            platformMode = PlatformMode.Normal,
        )

        assertEquals(DeviceCategory.Tablet, profile.category)
        assertEquals(NavigationStyle.Rail, profile.navigationStyle)
    }

    @Test
    fun hingeWinsOverScreenWidth() {
        val profile = DeviceClassifier.classify(
            widthClassDp = 600,
            smallestWidthDp = 500,
            hasHinge = true,
            platformMode = PlatformMode.Normal,
        )

        assertEquals(DeviceCategory.Foldable, profile.category)
    }

    @Test
    fun televisionModeUsesRailNavigation() {
        val profile = DeviceClassifier.classify(
            widthClassDp = 840,
            smallestWidthDp = 720,
            hasHinge = false,
            platformMode = PlatformMode.Television,
        )

        assertEquals(DeviceCategory.Television, profile.category)
        assertEquals(NavigationStyle.Rail, profile.navigationStyle)
    }
}
