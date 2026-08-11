import Testing
@testable import OpenReaderKit

@Test func classifiesPhoneAsCompact() {
    let profile = DeviceProfile.classify(width: 390, idiom: .phone)

    #expect(profile.category == .phone)
    #expect(profile.navigation == .tabs)
    #expect(!profile.supportsListDetail)
}

@Test func classifiesPadAsRegular() {
    let profile = DeviceProfile.classify(width: 1_024, idiom: .pad)

    #expect(profile.category == .tablet)
    #expect(profile.navigation == .sidebar)
    #expect(profile.supportsListDetail)
}

@Test func narrowPadWindowUsesTabs() {
    let profile = DeviceProfile.classify(width: 520, idiom: .pad)

    #expect(profile.category == .tablet)
    #expect(profile.navigation == .tabs)
}
