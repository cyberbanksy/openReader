import Testing
@testable import OpenReaderKit

@Test func normalizesServerAddress() throws {
    #expect(try ServerAddress.normalize("192.168.1.87:13378/") == "http://192.168.1.87:13378")
    #expect(try ServerAddress.normalize("https://books.example.com/") == "https://books.example.com")
}

@Test func rejectsUnsupportedServerScheme() {
    #expect(throws: ServerAddressError.unsupportedScheme) {
        try ServerAddress.normalize("file:///tmp/books")
    }
}

@Test func rejectsAddressWithoutHost() {
    #expect(throws: ServerAddressError.missingHost) {
        try ServerAddress.normalize("http://")
    }
}
