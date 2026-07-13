import Foundation
import SwiftData

#if DEBUG
/// Launch-argument rig for generating App Store screenshots headlessly.
/// Debug builds only; activated by "-screenshotMode". Usage:
///   xcrun simctl launch <sim> com.strive4it.greenmonkeys \
///       -screenshotMode -demoData -screen pattern
/// Screens: home, editor, session, morning, pattern, settings.
enum ScreenshotRig {
    static var isActive: Bool {
        ProcessInfo.processInfo.arguments.contains("-screenshotMode")
    }

    static var wantsDemoData: Bool {
        isActive && ProcessInfo.processInfo.arguments.contains("-demoData")
    }

    /// The value following "-screen".
    static var screen: String? {
        guard isActive else { return nil }
        let args = ProcessInfo.processInfo.arguments
        guard let index = args.firstIndex(of: "-screen"), index + 1 < args.count else { return nil }
        return args[index + 1]
    }

    // MARK: - Demo data

    @MainActor
    static func seedDemoDataIfNeeded(context: ModelContext) {
        guard wantsDemoData else { return }
        let existing = (try? context.fetch(FetchDescriptor<SessionPlan>())) ?? []
        guard existing.isEmpty else { return }

        CatalogService.rememberPromise("No karaoke")
        CatalogService.rememberCrime("Sang Wonderwall. Twice.")

        struct Promise {
            let kind: CommitmentKind
            let detail: String
            let broken: Bool
        }

        let calendar = Calendar.current
        func night(_ daysAgo: Int, hour: Int) -> Date {
            let day = calendar.date(byAdding: .day, value: -daysAgo, to: Date()) ?? Date()
            return calendar.date(bySettingHour: hour, minute: 0, second: 0, of: day) ?? day
        }

        func makePlan(_ occasion: String, daysAgo: Int, promises: [Promise],
                      score: Int?, crimes: [String], oneChange: String) {
            let plan = SessionPlan(
                occasion: occasion,
                sessionStart: night(daysAgo, hour: 19),
                plannedEnd: night(daysAgo, hour: 23),
                reminderOffsetsMinutes: [60, 120, 180]
            )
            context.insert(plan)
            for promise in promises {
                let commitment = Commitment(kind: promise.kind, detail: promise.detail)
                if score != nil { commitment.wasBroken = promise.broken }
                commitment.plan = plan
                plan.commitments.append(commitment)
            }
            if let score {
                let verdict = Verdict(score: score, crimes: crimes, oneChange: oneChange)
                verdict.plan = plan
                plan.verdict = verdict
            }
        }

        makePlan("Dave's 40th", daysAgo: 26, promises: [
            Promise(kind: .maxDrinks, detail: "4", broken: true),
            Promise(kind: .leaveBy, detail: "23:00", broken: true),
            Promise(kind: .noDriving, detail: "", broken: false),
        ], score: 4, crimes: ["Blacked out", "Threw up", "Lost phone / wallet / keys"],
           oneChange: "No tequila. Ever.")

        makePlan("Friday pub", daysAgo: 19, promises: [
            Promise(kind: .maxDrinks, detail: "5", broken: true),
            Promise(kind: .waterBetween, detail: "", broken: false),
        ], score: 2, crimes: ["Drunk texting"], oneChange: "Leave the phone in my coat.")

        makePlan("Work leaving drinks", daysAgo: 14, promises: [
            Promise(kind: .maxDrinks, detail: "3", broken: false),
            Promise(kind: .eatFirst, detail: "", broken: false),
        ], score: 0, crimes: [], oneChange: "")

        makePlan("Quiet one with Sam", daysAgo: 12, promises: [
            Promise(kind: .maxDrinks, detail: "4", broken: true),
            Promise(kind: .custom, detail: "No karaoke", broken: true),
            Promise(kind: .leaveBy, detail: "22:30", broken: true),
        ], score: 3, crimes: ["Insulted someone", "Sang Wonderwall. Twice."],
           oneChange: "\"Quiet one\" means quiet one.")

        makePlan("Sunday roast", daysAgo: 5, promises: [
            Promise(kind: .maxDrinks, detail: "2", broken: false),
            Promise(kind: .noDriving, detail: "", broken: false),
        ], score: 0, crimes: [], oneChange: "")

        // Last night — awaiting its verdict (the morning-after shot).
        makePlan("Pub quiz", daysAgo: 1, promises: [
            Promise(kind: .maxDrinks, detail: "4", broken: false),
            Promise(kind: .leaveBy, detail: "23:00", broken: false),
        ], score: nil, crimes: [], oneChange: "")

        // Tomorrow — upcoming (home + session shots). Negative daysAgo = future.
        makePlan("Marco's birthday", daysAgo: -1, promises: [
            Promise(kind: .maxDrinks, detail: "4", broken: false),
            Promise(kind: .waterBetween, detail: "", broken: false),
            Promise(kind: .leaveBy, detail: "23:00", broken: false),
            Promise(kind: .noDriving, detail: "", broken: false),
            Promise(kind: .custom, detail: "No karaoke", broken: false),
        ], score: nil, crimes: [], oneChange: "")

        try? context.save()
    }
}
#endif
