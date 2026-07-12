import SwiftUI

struct SettingsView: View {
    @AppStorage(SettingsKey.insultWord) private var insultWord = "idiot"
    @AppStorage(SettingsKey.brutality) private var brutality = Brutality.standard.rawValue
    @AppStorage(SettingsKey.morningAfterHour) private var morningAfterHour = 9
    @AppStorage(SettingsKey.appLockEnabled) private var appLockEnabled = true

    @State private var customWord = ""

    var body: some View {
        Form {
            Section("Your word") {
                Picker("Days since you were a…", selection: $insultWord) {
                    ForEach(AppSettings.insultPresets, id: \.self) { word in
                        Text(word).tag(word)
                    }
                    if !AppSettings.insultPresets.contains(insultWord) {
                        Text(insultWord).tag(insultWord)
                    }
                }
                HStack {
                    TextField("Or your own word…", text: $customWord)
                    Button("Use") {
                        let trimmed = customWord.trimmingCharacters(in: .whitespaces).lowercased()
                        guard !trimmed.isEmpty else { return }
                        insultWord = trimmed
                        customWord = ""
                    }
                    .disabled(customWord.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }

            Section {
                Picker("Brutality", selection: $brutality) {
                    ForEach(Brutality.allCases, id: \.rawValue) { level in
                        Text(level.label).tag(level.rawValue)
                    }
                }
                .pickerStyle(.segmented)
            } header: {
                Text("How hard should the Monkeys go?")
            } footer: {
                Text("Scales the jokes, not the debrief — every morning still ends with your one change.")
            }

            Section("The morning after") {
                Stepper("Debrief at \(morningAfterHour):00", value: $morningAfterHour, in: 5...14)
            }

            Section {
                Toggle("Lock app with Face ID", isOn: $appLockEnabled)
            } footer: {
                Text("Your videos never leave this phone: not iCloud, not backups, not anywhere. The lock keeps over-the-shoulder eyes out too.")
            }

            Section {
                Link("If this feels bigger than morning-after regret…",
                     destination: URL(string: "https://www.nhs.uk/live-well/alcohol-advice/") ?? URL(fileURLWithPath: "/"))
            } footer: {
                Text("Green Monkeys is a self-awareness tool, not treatment. If drinking is worrying you, the NHS page above is a good, judgement-free start.")
            }
        }
        .navigationTitle("Settings")
    }
}
