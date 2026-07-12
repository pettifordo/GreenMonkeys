import SwiftUI
import SwiftData

/// The pattern: idiot rate, repeat-offender promises, and the full record.
struct HistoryView: View {
    @Query(sort: \SessionPlan.sessionStart, order: .reverse) private var plans: [SessionPlan]

    private var judged: [SessionPlan] {
        plans.filter { $0.verdict != nil }
    }

    private var records: [CommitmentRecord] {
        judged.flatMap(\.commitments).compactMap { commitment in
            commitment.wasBroken.map { CommitmentRecord(kind: commitment.kind, wasBroken: $0) }
        }
    }

    var body: some View {
        List {
            Section("The score") {
                if let rate = PatternService.idiotRate(verdicts: judged.compactMap { $0.verdict?.wasIdiot }) {
                    LabeledContent("Sessions judged", value: "\(judged.count)")
                    LabeledContent("\(AppSettings.insultWord.capitalized) rate", value: rate.formatted(.percent.precision(.fractionLength(0))))
                } else {
                    Text("No judged sessions yet. The Monkeys await their first case.")
                        .foregroundStyle(.secondary)
                }
            }

            let offenders = PatternService.repeatOffenders(in: records)
            if !offenders.isEmpty {
                Section("Promises you keep breaking") {
                    ForEach(offenders, id: \.kind) { history in
                        VStack(alignment: .leading, spacing: 2) {
                            Text("\(history.kind.symbol) \(history.kind.label)")
                            Text("Promised \(history.timesPromised)× · broken \(history.timesBroken)× · kept \(history.timesKept)×")
                                .font(.caption)
                                .foregroundStyle(.secondary)
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
                            Text(plan.verdict?.wasIdiot == true ? "🙈" : "🎉")
                            VStack(alignment: .leading) {
                                Text(plan.occasion.isEmpty ? "A session" : plan.occasion)
                                Text(plan.sessionStart.formatted(date: .abbreviated, time: .omitted))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("The pattern")
    }
}
