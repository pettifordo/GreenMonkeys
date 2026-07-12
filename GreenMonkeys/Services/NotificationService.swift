import Foundation
import UserNotifications

/// Schedules session-night nudges and the morning-after summons.
/// All notifications are scheduled up-front when the plan is saved (SPEC §4)
/// and cancelled/rescheduled if the plan changes.
final class NotificationService: NSObject {
    static let shared = NotificationService()

    static let sessionCategory = "SESSION_REMINDER"
    static let morningCategory = "MORNING_AFTER"
    static let recordAction = "RECORD_VIDEO"

    /// Set by the app on launch; invoked when the user taps "Record video" on a nudge.
    var onRecordRequested: ((UUID) -> Void)?
    /// Invoked when the user opens a morning-after notification.
    var onMorningAfterOpened: ((UUID) -> Void)?

    // MARK: - Setup

    func configure() {
        let center = UNUserNotificationCenter.current()
        center.delegate = self

        let record = UNNotificationAction(
            identifier: Self.recordAction,
            title: "Record drunk-you 🎥",
            options: [.foreground]
        )
        let session = UNNotificationCategory(
            identifier: Self.sessionCategory,
            actions: [record],
            intentIdentifiers: []
        )
        let morning = UNNotificationCategory(
            identifier: Self.morningCategory,
            actions: [],
            intentIdentifiers: []
        )
        center.setNotificationCategories([session, morning])
    }

    func requestAuthorization() async -> Bool {
        let center = UNUserNotificationCenter.current()
        return (try? await center.requestAuthorization(options: [.alert, .sound, .badge])) ?? false
    }

    // MARK: - Scheduling

    /// Schedules every reminder for a plan plus the morning-after summons.
    func schedule(planID: UUID, occasion: String, sessionStart: Date, plannedEnd: Date,
                  reminderOffsetsMinutes: [Int], commitmentsSummary: String) async {
        await cancel(planID: planID)
        let center = UNUserNotificationCenter.current()

        for offset in reminderOffsetsMinutes {
            let fireDate = sessionStart.addingTimeInterval(TimeInterval(offset * 60))
            guard fireDate > Date() else { continue }

            let content = UNMutableNotificationContent()
            content.title = "🐒 The Green Monkeys"
            content.body = CharacterVoice.monkeySessionNudge(
                brutality: AppSettings.brutality,
                word: AppSettings.insultWord
            ) + "\n\nThe plan: \(commitmentsSummary)"
            content.sound = .default
            content.categoryIdentifier = Self.sessionCategory
            content.interruptionLevel = .timeSensitive
            content.userInfo = ["planID": planID.uuidString]

            let request = UNNotificationRequest(
                identifier: "session-\(planID.uuidString)-\(offset)",
                content: content,
                trigger: calendarTrigger(for: fireDate)
            )
            try? await center.add(request)
        }

        // Morning after: next morning after plannedEnd at the configured hour.
        var morning = Calendar.current.startOfDay(for: plannedEnd.addingTimeInterval(24 * 3600))
        morning = Calendar.current.date(bySettingHour: AppSettings.morningAfterHour, minute: 0, second: 0, of: morning) ?? morning
        if morning > Date() {
            let content = UNMutableNotificationContent()
            content.title = "🫣 Captain Paranoia"
            content.body = "Morning. Time for the debrief on \(occasion). The Monkeys have the tape ready."
            content.sound = .default
            content.categoryIdentifier = Self.morningCategory
            content.userInfo = ["planID": planID.uuidString]

            let request = UNNotificationRequest(
                identifier: "morning-\(planID.uuidString)",
                content: content,
                trigger: calendarTrigger(for: morning)
            )
            try? await center.add(request)
        }
    }

    func cancel(planID: UUID) async {
        let center = UNUserNotificationCenter.current()
        let pending = await center.pendingNotificationRequests()
        let ids = pending.map(\.identifier).filter { $0.contains(planID.uuidString) }
        center.removePendingNotificationRequests(withIdentifiers: ids)
    }

    private func calendarTrigger(for date: Date) -> UNCalendarNotificationTrigger {
        let components = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: date)
        return UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
    }
}

// MARK: - UNUserNotificationCenterDelegate

extension NotificationService: UNUserNotificationCenterDelegate {
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        [.banner, .sound, .list]
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse) async {
        guard let idString = response.notification.request.content.userInfo["planID"] as? String,
              let planID = UUID(uuidString: idString) else { return }

        let category = response.notification.request.content.categoryIdentifier
        if response.actionIdentifier == Self.recordAction {
            onRecordRequested?(planID)
        } else if category == Self.morningCategory {
            onMorningAfterOpened?(planID)
        }
    }
}
