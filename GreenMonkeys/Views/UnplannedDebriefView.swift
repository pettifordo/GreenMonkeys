import SwiftUI
import SwiftData

/// Entry point for a morning-after with no pre-existing plan (SPEC §4).
/// Creates a retrospective session — no commitments, no reminders — and goes
/// straight to the debrief. Regret shouldn't need a booking.
struct UnplannedDebriefView: View {
    @Environment(\.modelContext) private var modelContext
    /// Home's navigation path: the debrief is pushed as a value route so
    /// "Finish — go face the day" can unwind the whole flow with a path reset.
    @Binding var path: NavigationPath

    @State private var occasion = ""
    @State private var night: Date = Calendar.current.date(byAdding: .day, value: -1, to: Date()) ?? Date()

    var body: some View {
        Form {
            Section {
                Text("No plan, big night, rough morning? The Monkeys will still take your confession.")
                    .font(.headline)
            }

            Section {
                TextField("What was the occasion? (optional)", text: $occasion)
                DatePicker(
                    "Which night?",
                    selection: $night,
                    in: ...Date(),
                    displayedComponents: .date
                )
            } footer: {
                Text("No promises were made, so there's nothing to judge against — just the verdict, the videos, and your one change.")
            }

            Section {
                Button {
                    startDebrief()
                } label: {
                    Text("Face the music")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
            }
        }
        .navigationTitle("Unplanned night")
    }

    private func startDebrief() {
        let calendar = Calendar.current
        let start = calendar.date(bySettingHour: 20, minute: 0, second: 0, of: night) ?? night
        let end = calendar.date(bySettingHour: 23, minute: 59, second: 0, of: night) ?? night
        let plan = SessionPlan(
            occasion: occasion.isEmpty ? "Unplanned night" : occasion,
            sessionStart: start,
            plannedEnd: end,
            reminderOffsetsMinutes: []
        )
        modelContext.insert(plan)
        try? modelContext.save()
        path.append(HomeRoute.debrief(plan))
    }
}
