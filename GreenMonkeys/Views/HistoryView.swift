import SwiftUI
import SwiftData
import Charts

/// The pattern: the composite idiot score, repeat-offender promises, the
/// booze-crime tallies, and the full record.
struct HistoryView: View {
    @Query(sort: \SessionPlan.sessionStart, order: .reverse) private var plans: [SessionPlan]

    @AppStorage(SettingsKey.insultWord) private var insultWord = "idiot"
    @AppStorage(SettingsKey.seedLongestStreak) private var seedLongestStreak = 0

    @State private var period: ChartPeriod = .night

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

    private var longestStreak: Int {
        max(
            StreakService.longestStreak(
                idiotDates: plans.filter { ($0.verdict?.effectiveScore ?? 0) > 0 }.map(\.sessionStart),
                firstUseDate: AppSettings.firstUseDate
            ),
            seedLongestStreak
        )
    }

    private var nightRecords: [NightRecord] {
        judged.map { plan in
            NightRecord(
                date: plan.sessionStart,
                score: plan.verdict?.effectiveScore ?? 0,
                crimes: plan.verdict?.crimes.count ?? 0,
                brokenPromises: plan.commitments.filter { $0.wasBroken == true }.count
            )
        }
    }

    private var periodPoints: [PeriodPoint] {
        PatternService.aggregate(nightRecords, by: period)
    }

    private var trendLine: TrendLine? {
        PatternService.trend(
            dates: periodPoints.map(\.periodStart),
            values: periodPoints.map(\.averageScore)
        )
    }

    /// Two points for the dashed forecast: fitted value at the last bucket,
    /// projected value one horizon ahead.
    private var forecastPoints: [(date: Date, value: Double)] {
        guard let trendLine, let last = periodPoints.last?.periodStart else { return [] }
        let horizon: DateComponents
        switch period {
        case .night: horizon = DateComponents(day: 7)
        case .week:  horizon = DateComponents(weekOfYear: 2)
        case .month: horizon = DateComponents(month: 2)
        case .year:  horizon = DateComponents(year: 1)
        }
        guard let future = Calendar.current.date(byAdding: horizon, to: last) else { return [] }
        return [(last, trendLine.value(at: last)), (future, trendLine.value(at: future))]
    }

    private var forecastCommentary: String? {
        guard let trendLine else { return nil }
        if trendLine.slope < -0.005 {
            return "Forecast: improving. The Monkeys are cautiously proud."
        }
        if trendLine.slope > 0.005 {
            return "Forecast: heading the wrong way. The Monkeys are sharpening their pencils."
        }
        return "Forecast: flat. Consistency, of a sort."
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
                    LabeledContent("Longest clean streak", value: "\(longestStreak) days")
                } else {
                    Text("No judged sessions yet. The Monkeys await their first case.")
                        .foregroundStyle(.secondary)
                }
            }

            if nightRecords.count >= 2 {
                Section {
                    Picker("Summary", selection: $period) {
                        ForEach(ChartPeriod.allCases) { option in
                            Text(option.label).tag(option)
                        }
                    }
                    .pickerStyle(.segmented)

                    Chart {
                        ForEach(periodPoints) { point in
                            LineMark(
                                x: .value("When", point.periodStart, unit: period.chartUnit),
                                y: .value("Score", point.averageScore),
                                series: .value("Series", "Score")
                            )
                            .interpolationMethod(.catmullRom)
                            .foregroundStyle(.blue)
                            .lineStyle(StrokeStyle(lineWidth: 2.5))

                            PointMark(
                                x: .value("When", point.periodStart, unit: period.chartUnit),
                                y: .value("Score", point.averageScore)
                            )
                            .foregroundStyle(.blue)
                            .symbolSize(70)
                        }
                        ForEach(Array(forecastPoints.enumerated()), id: \.offset) { _, forecast in
                            LineMark(
                                x: .value("When", forecast.date, unit: period.chartUnit),
                                y: .value("Score", forecast.value),
                                series: .value("Series", "Forecast")
                            )
                            .lineStyle(StrokeStyle(lineWidth: 1.5, dash: [5, 4]))
                            .foregroundStyle(.secondary)
                        }
                    }
                    .chartYScale(domain: 0...5)
                    .chartYAxis { AxisMarks(values: [0, 1, 2, 3, 4, 5]) }
                    .frame(height: 190)
                    .padding(.vertical, 4)
                } header: {
                    Text("\(insultWord.capitalized) score over time")
                } footer: {
                    Text("Lower is better; zero means you behaved."
                         + (forecastCommentary.map { " Dashed line — \($0)" } ?? ""))
                }

                Section {
                    Chart(periodPoints) { point in
                        BarMark(
                            x: .value("When", point.periodStart, unit: period.chartUnit),
                            y: .value("Count", point.crimes)
                        )
                        .foregroundStyle(by: .value("Type", "Booze crimes"))
                        .cornerRadius(3)
                        BarMark(
                            x: .value("When", point.periodStart, unit: period.chartUnit),
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
                    Text("Misdeeds per \(period.label.lowercased())")
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
