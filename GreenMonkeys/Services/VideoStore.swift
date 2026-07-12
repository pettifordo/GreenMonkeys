import Foundation

/// Owns the on-disk video files. Videos are radioactive (SPEC §5): complete
/// file protection, excluded from backup, never exported.
struct VideoStore {
    static let shared = VideoStore()

    private let directory: URL

    init() {
        let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        directory = support.appendingPathComponent("Videos", isDirectory: true)
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        excludeFromBackup(directory)
    }

    func url(for fileName: String) -> URL {
        directory.appendingPathComponent(fileName)
    }

    /// Moves a freshly recorded temp file into protected storage. Returns the stored file name.
    func store(temporaryURL: URL) throws -> String {
        let fileName = UUID().uuidString + ".mov"
        let destination = url(for: fileName)
        try FileManager.default.moveItem(at: temporaryURL, to: destination)
        try? FileManager.default.setAttributes(
            [.protectionKey: FileProtectionType.complete],
            ofItemAtPath: destination.path
        )
        excludeFromBackup(destination)
        return fileName
    }

    /// Deletion only ever happens on explicit user action (hard rule 3).
    func delete(fileName: String) {
        try? FileManager.default.removeItem(at: url(for: fileName))
    }

    func exists(fileName: String) -> Bool {
        FileManager.default.fileExists(atPath: url(for: fileName).path)
    }

    private func excludeFromBackup(_ target: URL) {
        var url = target
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        try? url.setResourceValues(values)
    }
}
