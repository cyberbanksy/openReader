import Foundation

public enum ReaderDeviceIdiom: Sendable {
    case phone
    case pad
    case mac
    case vision
    case unknown
}

public enum ReaderDeviceCategory: Sendable {
    case phone
    case tablet
    case desktop
    case spatial
}

public enum ReaderNavigation: Sendable {
    case tabs
    case sidebar
}

public struct DeviceProfile: Equatable, Sendable {
    public let category: ReaderDeviceCategory
    public let navigation: ReaderNavigation
    public let supportsListDetail: Bool

    public static func classify(width: Double, idiom: ReaderDeviceIdiom) -> DeviceProfile {
        let category: ReaderDeviceCategory = switch idiom {
        case .phone: .phone
        case .pad: .tablet
        case .mac: .desktop
        case .vision: .spatial
        case .unknown: width >= 700 ? .tablet : .phone
        }
        return DeviceProfile(
            category: category,
            navigation: width >= 600 ? .sidebar : .tabs,
            supportsListDetail: width >= 900
        )
    }
}

public enum BookFormat: String, CaseIterable, Identifiable, Sendable {
    case audiobook
    case ebook

    public var id: Self { self }
}

public struct LibraryBook: Identifiable, Equatable, Sendable {
    public let id: String
    public let title: String
    public let creator: String
    public let format: BookFormat
    public let progress: Double
    public let colorSeed: Int

    public init(
        id: String,
        title: String,
        creator: String,
        format: BookFormat,
        progress: Double = 0,
        colorSeed: Int = 0
    ) {
        self.id = id
        self.title = title
        self.creator = creator
        self.format = format
        self.progress = progress
        self.colorSeed = colorSeed
    }
}

public enum DemoLibrary {
    public static let books = [
        LibraryBook(
            id: "alice-audio",
            title: "Alice's Adventures in Wonderland",
            creator: "Lewis Carroll",
            format: .audiobook,
            progress: 0.34,
            colorSeed: 0
        ),
        LibraryBook(
            id: "alice-ebook",
            title: "Alice's Adventures in Wonderland",
            creator: "Lewis Carroll",
            format: .ebook,
            progress: 0.18,
            colorSeed: 1
        ),
        LibraryBook(
            id: "secret-garden",
            title: "The Secret Garden",
            creator: "Frances Hodgson Burnett",
            format: .ebook,
            colorSeed: 2
        ),
        LibraryBook(
            id: "treasure-island",
            title: "Treasure Island",
            creator: "Robert Louis Stevenson",
            format: .audiobook,
            colorSeed: 3
        ),
    ]
}
