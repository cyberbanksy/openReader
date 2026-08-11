// swift-tools-version: 6.2

import PackageDescription

let package = Package(
    name: "OpenReaderApple",
    platforms: [
        .iOS(.v18),
        .macOS(.v15),
    ],
    products: [
        .library(name: "OpenReaderKit", targets: ["OpenReaderKit"]),
    ],
    targets: [
        .target(name: "OpenReaderKit"),
        .testTarget(name: "OpenReaderKitTests", dependencies: ["OpenReaderKit"]),
    ]
)
