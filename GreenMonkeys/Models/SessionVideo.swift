import Foundation
import SwiftData

enum VideoKind: String, CaseIterable {
    case plan
    case drunk
    case morningAfter

    var label: String {
        switch self {
        case .plan:         return "The Plan"
        case .drunk:        return "The Evidence"
        case .morningAfter: return "The Morning After"
        }
    }
}

@Model
final class SessionVideo {
    var id: UUID = UUID()
    var kind: String = VideoKind.plan.rawValue
    /// File name inside VideoStore's directory. Videos never leave the device.
    var fileName: String = ""
    var recordedAt: Date = Date()
    var plan: SessionPlan?

    init(kind: VideoKind, fileName: String) {
        self.id = UUID()
        self.kind = kind.rawValue
        self.fileName = fileName
        self.recordedAt = Date()
    }
}
