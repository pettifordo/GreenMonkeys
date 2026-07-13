import SwiftUI
import SwiftData

/// The pattern: the composite idiot score, repeat-offender promises, the
/// booze-crime tallies, and the full record.
struct HistoryView: View {
    @Query(sort: \SessionPlan.sessionStart, order: .reverse) private var plans: [SessionPlan]

    @AppStorage(SettingsKey.insultWord) private var insultWord = "idiot"

    private var judged: [SessionPlan] {
        plans.filter { $0.verdict != nil }
    }

    private var scores: [Int] {
        judged.compactMap { $0.verdict?.effectiveScore }
    }

    private var records: [CommitmentRecord] {
        judged.flatMap(\.commitments).compactMap { commitment in
            commitment.wasBroken.map {
                CommitmentRecord(key: commitment.patternKey, label: commitment.patternLabel, wasBroken: $0)
            }
        }
    }

    var body: some View {
        List {
            Section("The score") {
                if let rate = PatternService.idiotRate(scores: scores),
                   let average = PatternService.averageScore(scores: scores) {
                    LabeledContent("Sessions judged", value: "\(judged.count)")
                    LabeledContent("\(insultWord.capitalized) rate",
                                   value: rate.formatted(.percent.precision(.fractionLength(0))))
                    LabeledContent("Average \(insultWord) score",
                                   value: "\(average.formatted(.number.precision(.fractionLength(1)))) / 5")
                    if let worst = scores.max() {
                        LabeledContent("Personal best (worst)", value: "\(worst) / 5")
                    }
                } else {
                    Text("No judged sessions yet. The Monkeys await their first case.")
                        .foregroundStyle(.secondary)
                }
            }

            let offenders = PatternService.repeatOffenders(in: records)
            if !offenders.isEmpty {
                Section("Promises you keep breaking") {
                    ForEach(offenders, id: \.key) { history in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(history.label)
                            Text("Promised \(history.timesPromised)× · broken \(history.timesBroken)× · kept \(history.timesKept)×")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }

            let crimes = PatternService.crimeCounts(crimeLists: judged.compactMap { $0.verdict?.crimes })
            if !crimes.isEmpty {
                Section("The charge sheet, lifetime edition") {
                    ForEach(crimes, id: \.crime) { entry in
                        HStack {
                            Text(entry.crime)
                            Spacer()
                            Text("\(entry.count)×")
                                .font(.headline)
                                .foregroundStyle(.red)
                        }
                    }
                }
            }

            Section("The record") {
                ForEach(judged) { plan in
                    NavigationLink {
                        PlanDetailView(plan: plan)
                    } label: {
                        HStack {
                            Text((plan.verdict?.effectiveScore ?? 0) == 0 ? "🎉" : "🙈")
                            VStack(alignment: .leading) {
                                Text(plan.occasion.isEmpty ? "A session" : plan.occasion)
                                Text(plan.sessionStart.formatted(date: .abbreviated, time: .omitted))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            if let score = plan.verdict?.effectiveScore, score > 0 {
                                Text("\(score)/5")
                                    .font(.caption.bold())
                                    .foregroundStyle(.red)
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("The pattern")
    }
}
