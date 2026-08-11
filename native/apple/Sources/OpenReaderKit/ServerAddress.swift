import Foundation

public enum ServerAddressError: Error, Equatable {
    case empty
    case missingHost
    case unsupportedScheme
}

public enum ServerAddress {
    public static func normalize(_ value: String) throws -> String {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { throw ServerAddressError.empty }

        let candidate = trimmed.contains("://") ? trimmed : "http://\(trimmed)"
        guard let components = URLComponents(string: candidate) else {
            throw ServerAddressError.missingHost
        }
        guard components.scheme == "http" || components.scheme == "https" else {
            throw ServerAddressError.unsupportedScheme
        }
        guard let host = components.host, !host.isEmpty else {
            throw ServerAddressError.missingHost
        }
        return candidate.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
    }
}
