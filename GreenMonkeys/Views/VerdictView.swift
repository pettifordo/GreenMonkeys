import SwiftUI
import SwiftData

/// The dedicated verdict screen, presented after "Deliver the verdict" (and when
/// revisiting a judged session): the roast, the monkey-free closer, and the exit.
struct VerdictView: View {
    @Environment(AppRouter.self) private var router
    let plan: SessionPlan

    @AppStorage(SettingsKey.insultWord) private var insultWord = "idiot"

    private var roast: CharacterVoice.Roast? {
        guard let verdict = plan.verdict else { return nil }
        return CharacterVoice.roast(
            score: verdict.effectiveScore,
            crimes: verdict.crimes,
            brokenPromises: plan.commitments.filter { $0.wasBroken == true }.count,
            brutality: AppSettings.brutality,
            word: insultWord
        )
    }

    var body: some View {
        List {
            if let roast {
                Section {
                    Text(roast.opener)
                        .font(.title3.bold())
                    ForEach(roast.charges, id: \.self) { charge in
                        HStack(alignment: .top, spacing: 8) {
                            Text("🚨")
                            Text(charge)
                        }
                        .font(.subheadline)
                    }
                    if let promisesLine = roast.promisesLine {
                        Text(promisesLine)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                } header: {
                    Text("🐒 The roast")
                }

                Section {
                    Text(roast.closer)
                } header: {
                    Text("And finally, monkey-free")
                }

                if let oneChange = plan.verdict?.oneChange,
                   !oneChange.trimmingCharacters(in: .whitespaces).isEmpty {
                    Section("Your one change") {
                        Label(oneChange, systemImage: "arrow.turn.up.right")
                    }
                }
            }

            Section {
                Button {
                    router.finishToHome()
                } label: {
                    Text("Finish — go face the day")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
            }
        }
        .navigationTitle("The verdict")
        .navigationBarTitleDisplayMode(.inline)
        .interactiveDismissDisabled()
    }
}
