package com.orgista.openreader.domain

enum class DeviceCategory {
    Phone,
    Foldable,
    Tablet,
    Desktop,
    Television,
    Car,
}

enum class NavigationStyle {
    BottomBar,
    Rail,
}

enum class PlatformMode {
    Normal,
    Television,
    Car,
    Desktop,
}

data class DeviceProfile(
    val category: DeviceCategory,
    val navigationStyle: NavigationStyle,
    val widthClassDp: Int,
    val supportsListDetail: Boolean,
)

object DeviceClassifier {
    fun classify(
        widthClassDp: Int,
        smallestWidthDp: Int,
        hasHinge: Boolean,
        platformMode: PlatformMode,
    ): DeviceProfile {
        val category = when {
            platformMode == PlatformMode.Television -> DeviceCategory.Television
            platformMode == PlatformMode.Car -> DeviceCategory.Car
            platformMode == PlatformMode.Desktop -> DeviceCategory.Desktop
            hasHinge -> DeviceCategory.Foldable
            smallestWidthDp >= 600 || widthClassDp >= 840 -> DeviceCategory.Tablet
            else -> DeviceCategory.Phone
        }
        val usesRail = widthClassDp >= 600 || category in setOf(
            DeviceCategory.Tablet,
            DeviceCategory.Desktop,
            DeviceCategory.Television,
        )
        return DeviceProfile(
            category = category,
            navigationStyle = if (usesRail) NavigationStyle.Rail else NavigationStyle.BottomBar,
            widthClassDp = widthClassDp,
            supportsListDetail = widthClassDp >= 840,
        )
    }
}
