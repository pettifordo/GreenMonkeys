import WidgetKit
import SwiftUI

@main
struct GreenMonkeysWidgets: WidgetBundle {
    var body: some Widget {
        StreakWidget()
    }
}

// MARK: - Timeline

struct StreakEntry: TimelineEntry {
    let date: Date
    let days: Int
    let word: String
    let hasHistory: Bool
}

struct StreakProvider: TimelineProvider {
    func placeholder(in context: Context) -> StreakEntry {
        StreakEntry(date: Date(), days: 12, word: "idiot", hasHistory: true)
    }

    func getSnapshot(in context: Context, completion: @escaping (StreakEntry) -> Void) {
        completion(entry(for: Date()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<StreakEntry>) -> Void) {
        // One entry now, then one at each of the next 7 midnights so the count
        // ticks over without the app being opened.
        var entries = [entry(for: Date())]
        let calendar = Calendar.current
        for dayOffset in 1...7 {
            if let midnight = calendar.date(byAdding: .day, value: dayOffset, to: calendar.startOfDay(for: Date())) {
                entries.append(entry(for: midnight))
            }
        }
        completion(Timeline(entries: entries, policy: .atEnd))
    }

    private func entry(for date: Date) -> StreakEntry {
        let snapshot = StreakSnapshot.load()
        return StreakEntry(
            date: date,
            days: snapshot.days(now: date),
            word: snapshot.insultWord,
            hasHistory: snapshot.hasIdiotHistory
        )
    }
}

// MARK: - Widget

struct StreakWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "StreakWidget", provider: StreakProvider()) { entry in
            StreakWidgetView(entry: entry)
                .containerBackground(for: .widget) {
                    Color(.systemBackground)
                }
        }
        .configurationDisplayName("Days since…")
        .description("Your streak, watched over by the Green Monkeys.")
        .supportedFamilies([.systemSmall, .accessoryCircular, .accessoryRectangular, .accessoryInline])
    }
}

struct StreakWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: StreakEntry

    private var isAtZero: Bool { entry.days == 0 && entry.hasHistory }

    private var article: String {
        "aeiou".contains(entry.word.lowercased().first ?? " ") ? "an" : "a"
    }

    var body: some View {
        switch family {
        case .accessoryCircular:
            VStack(spacing: 0) {
                Text("🐒")
                    .font(.caption2)
                Text("\(entry.days)")
                    .font(.title2.bold())
            }
        case .accessoryRectangular:
            VStack(alignment: .leading, spacing: 1) {
                Text("🐒 Days since \(article) \(entry.word)")
                    .font(.caption2)
                Text("\(entry.days)")
                    .font(.title.bold())
            }
        case .accessoryInline:
            Text("🐒 \(entry.days) days \(entry.word)-free")
        default:
            VStack(spacing: 4) {
                Text("Days since you were \(article)…")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                Text(entry.word)
                    .font(.headline)
                Text("\(entry.days)")
                    .font(.system(size: 44, weight: .black, design: .rounded))
                    .foregroundStyle(isAtZero ? .red : .green)
                Text("🐒")
                    .font(.caption)
            }
        }
    }
}
