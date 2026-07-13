import SwiftUI
import SwiftData
import Charts

/// One judged night, flattened for charting.
private struct NightPoint: Identifiable {
    var id: Date { date }
    let date: Date
    let score: Int
    let crimes: Int
    let brokenPromises: Int
}

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

    private var nightPoints: [NightPoint] {
        judged
            .sorted { $0.sessionStart < $1.sessionStart }
            .map { plan in
                NightPoint(
                    date: plan.sessionStart,
                    score: plan.verdict?.effectiveScore ?? 0,
                    crimes: plan.verdict?.crimes.count ?? 0,
                    brokenPromises: plan.commitments.filter { $0.wasBroken == true }.count
                )
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

            if nightPoints.count >= 2 {
                Section {
                    Chart {
                        ForEach(nightPoints) { point in
                            if point.score == 0 {
                                // A zero-height bar is invisible — clean nights get a green dot on the axis.
                                PointMark(
                                    x: .value("Night", point.date, unit: .day),
                                    y: .value("Score", 0)
                                )
                                .foregroundStyle(.green)
                                .symbolSize(110)
                            } else {
                                BarMark(
                                    x: .value("Night", point.date, unit: .day),
                                    y: .value("Score", point.score)
                                )
                                .foregroundStyle(scoreColor(point.score))
                                .cornerRadius(3)
                            }
                        }
                        if let average = PatternService.averageScore(scores: scores) {
                            RuleMark(y: .value("Average", average))
                                .lineStyle(StrokeStyle(lineWidth: 1, dash: [5, 4]))
                                .foregroundStyle(.secondary)
                                .annotation(position: .top, alignment: .trailing) {
                                    Text("average \(average.formatted(.number.precision(.fractionLength(1))))")
                                        .font(.caption2)
                                        .foregroundStyle(.secondary)
                                }
                        }
                    }
                    .chartYScale(domain: 0...5)
                    .chartYAxis { AxisMarks(values: [0, 1, 2, 3, 4, 5]) }
                    .frame(height: 190)
                    .padding(.vertical, 4)
                } header: {
                    Text("\(insultWord.capitalized) score over time")
                } footer: {
                    Text("Green nights keep the streak. The dotted line is who you are on average — aim under it.")
                }

                Section {
                    Chart(nightPoints) { point in
                        BarMark(
                            x: .value("Night", point.date, unit: .day),
                            y: .value("Count", point.crimes)
                        )
                        .foregroundStyle(by: .value("Type", "Booze crimes"))
                        .cornerRadius(3)
                        BarMark(
                            x: .value("Night", point.date, unit: .day),
                            y: .value("Count", point.brokenPromises)
                        )
                        .foregroundStyle(by: .value("Type", "Broken promises"))
                        .cornerRadius(3)
                    }
                    .chartForegroundStyleScale([
                        "Booze crimes": Color.red,
                        "Broken promises": Color.orange,
                    ])
                    .frame(height: 170)
                    .padding(.vertical, 4)
                } header: {
                    Text("Misdeeds per night")
                } footer: {
                    Text("Crimes confessed plus promises broken, stacked. Flat is the goal; the Monkeys prefer drama.")
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

    private func scoreColor(_ score: Int) -> Color {
        score == 0 ? .green : .red.opacity(0.35 + Double(score) * 0.13)
    }
}
