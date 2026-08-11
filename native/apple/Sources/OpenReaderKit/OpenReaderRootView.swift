import SwiftUI
#if os(iOS)
import UIKit
#endif

public struct OpenReaderRootView: View {
    @State private var selection: ReaderSection = .home
    @State private var selectedBook: LibraryBook?
    @State private var searchText = ""

    public init() {}

    public var body: some View {
        GeometryReader { proxy in
            let profile = DeviceProfile.classify(
                width: proxy.size.width,
                idiom: currentIdiom
            )
            Group {
                if profile.navigation == .sidebar {
                    regularLayout(profile: profile)
                } else {
                    compactLayout
                }
            }
        }
        .tint(.openReaderCoral)
    }

    private var compactLayout: some View {
        TabView(selection: $selection) {
            ForEach(ReaderSection.allCases) { section in
                NavigationStack {
                    LibraryView(
                        section: section,
                        selectedBook: $selectedBook,
                        searchText: $searchText
                    )
                }
                .tag(section)
                .tabItem { Label(section.title, systemImage: section.symbol) }
            }
        }
    }

    @ViewBuilder
    private func regularLayout(profile: DeviceProfile) -> some View {
        NavigationSplitView {
            List(ReaderSection.allCases, selection: $selection) { section in
                Label(section.title, systemImage: section.symbol)
                    .tag(section)
            }
            .navigationTitle("OpenReader")
        } content: {
            LibraryView(
                section: selection,
                selectedBook: $selectedBook,
                searchText: $searchText
            )
        } detail: {
            if let selectedBook {
                BookDetailView(book: selectedBook)
            } else {
                ContentUnavailableView(
                    "Choose a Book",
                    systemImage: "books.vertical",
                    description: Text("Select a title from your library.")
                )
            }
        }
        .navigationSplitViewStyle(.balanced)
    }

    private var currentIdiom: ReaderDeviceIdiom {
        #if os(iOS)
        switch UIDevice.current.userInterfaceIdiom {
        case .phone: .phone
        case .pad: .pad
        case .vision: .vision
        default: .unknown
        }
        #elseif os(macOS)
        .mac
        #else
        .unknown
        #endif
    }
}

private enum ReaderSection: String, CaseIterable, Identifiable {
    case home
    case library
    case listen
    case read

    var id: Self { self }
    var title: String { rawValue.capitalized }
    var symbol: String {
        switch self {
        case .home: "house.fill"
        case .library: "books.vertical.fill"
        case .listen: "headphones"
        case .read: "book.fill"
        }
    }
}

private struct LibraryView: View {
    let section: ReaderSection
    @Binding var selectedBook: LibraryBook?
    @Binding var searchText: String

    private var books: [LibraryBook] {
        DemoLibrary.books.filter { book in
            let matchesSection = switch section {
            case .listen: book.format == .audiobook
            case .read: book.format == .ebook
            default: true
            }
            let matchesSearch = searchText.isEmpty ||
                book.title.localizedCaseInsensitiveContains(searchText) ||
                book.creator.localizedCaseInsensitiveContains(searchText)
            return matchesSection && matchesSearch
        }
    }

    var body: some View {
        ScrollView {
            LazyVGrid(
                columns: [GridItem(.adaptive(minimum: 142, maximum: 190), spacing: 18)],
                spacing: 24
            ) {
                ForEach(books) { book in
                    Button {
                        selectedBook = book
                    } label: {
                        BookTile(book: book)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(24)
        }
        .navigationTitle(section == .home ? "OpenReader" : section.title)
        .searchable(text: $searchText, prompt: "Search your library")
    }
}

private struct BookTile: View {
    let book: LibraryBook

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            ZStack(alignment: .bottom) {
                RoundedRectangle(cornerRadius: 6)
                    .fill(Color.coverPalette[abs(book.colorSeed) % Color.coverPalette.count])
                    .aspectRatio(0.72, contentMode: .fit)
                    .overlay(alignment: .topLeading) {
                        Image(systemName: book.format == .audiobook ? "headphones" : "book.fill")
                            .font(.title2)
                            .foregroundStyle(.white.opacity(0.9))
                            .padding(14)
                    }
                    .overlay(alignment: .bottomLeading) {
                        Text(book.title)
                            .font(.headline)
                            .foregroundStyle(.white)
                            .lineLimit(4)
                            .padding(14)
                    }
                if book.progress > 0 {
                    ProgressView(value: book.progress)
                        .tint(.openReaderCoral)
                }
            }
            Text(book.title)
                .font(.headline)
                .lineLimit(2)
                .multilineTextAlignment(.leading)
            Text(book.creator)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
        .accessibilityElement(children: .combine)
    }
}

private struct BookDetailView: View {
    let book: LibraryBook

    var body: some View {
        VStack(spacing: 18) {
            BookTile(book: book)
                .frame(maxWidth: 240)
            Button(book.format == .audiobook ? "Play" : "Read", systemImage: book.format == .audiobook ? "play.fill" : "book.fill") {}
                .buttonStyle(.borderedProminent)
        }
        .padding(32)
        .navigationTitle(book.title)
    }
}

private extension Color {
    static let openReaderCoral = Color(red: 1.0, green: 0.42, blue: 0.40)
    static let coverPalette: [Color] = [
        Color(red: 0.15, green: 0.33, blue: 0.49),
        Color(red: 0.36, green: 0.16, blue: 0.26),
        Color(red: 0.16, green: 0.44, blue: 0.38),
        Color(red: 0.45, green: 0.35, blue: 0.17),
        Color(red: 0.30, green: 0.31, blue: 0.49),
    ]
}
