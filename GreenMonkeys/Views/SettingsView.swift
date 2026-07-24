import SwiftUI
import WidgetKit

struct SettingsView: View {
    @AppStorage(SettingsKey.insultWord) private var insultWord = "idiot"
    @AppStorage(SettingsKey.sessionNoun) private var sessionNoun = "Session"
    @AppStorage(SettingsKey.brutality) private var brutality = Brutality.standard.rawValue
    @AppStorage(SettingsKey.morningAfterHour) private var morningAfterHour = 9
    @AppStorage(SettingsKey.appLockEnabled) private var appLockEnabled = true
    @AppStorage(SettingsKey.seedLongestStreak) private var seedLongestStreak = 0
    @AppStorage(SettingsKey.streakStartTime) private var streakStartTime = 0.0
    @AppStorage(SettingsKey.streakStartEdits) private var streakStartEdits = 0

    @State private var customWord = ""
    @State private var customNoun = ""
    @State private var customPromises: [String] = CatalogService.customPromises
    @State private var customCrimes: [String] = CatalogService.customCrimes
    @State private var editingStreakStart = false
    @State private var draftStreakStart = Date()
    @State private var confirmingStreakMove = false
    @State private var replayOnboarding = false

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
                Picker("Plan a…", selection: $sessionNoun) {
                    ForEach(AppSettings.sessionNounPresets, id: \.self) { noun in
                        Text(noun).tag(noun)
                    }
                    if !AppSettings.sessionNounPresets.contains(sessionNoun) {
                        Text(sessionNoun).tag(sessionNoun)
                    }
                }
                HStack {
                    TextField("Or your own name for it…", text: $customNoun)
                    Button("Use") {
                        let trimmed = customNoun.trimmingCharacters(in: .whitespaces)
                        guard !trimmed.isEmpty else { return }
                        sessionNoun = trimmed
                        customNoun = ""
                    }
                    .disabled(customNoun.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            } header: {
                Text("What do you call one?")
            } footer: {
                Text("Drives the plan button and empty states — \"Plan a \(sessionNoun)\", \"No \(sessionNoun) planned\".")
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
                Text("Scales the jokes, not the debrief.")
            }

            Section {
                if customPromises.isEmpty {
                    Text("Custom promises you add while planning appear here.")
                        .foregroundStyle(.secondary)
                }
                ForEach(customPromises, id: \.self) { promise in
                    Text("🤙 \(promise)")
                }
                .onDelete { offsets in
                    for index in offsets {
                        CatalogService.removeCustomPromise(customPromises[index])
                    }
                    customPromises = CatalogService.customPromises
                }
            } header: {
                Text("Your promise list")
            } footer: {
                Text("Swipe to remove. Past sessions keep their record either way.")
            }

            Section {
                if customCrimes.isEmpty {
                    Text("Crimes you confess to that aren't on the standard charge sheet appear here.")
                        .foregroundStyle(.secondary)
                }
                ForEach(customCrimes, id: \.self) { crime in
                    Text("🚨 \(crime)")
                }
                .onDelete { offsets in
                    for index in offsets {
                        CatalogService.removeCustomCrime(customCrimes[index])
                    }
                    customCrimes = CatalogService.customCrimes
                }
            } header: {
                Text("Your booze-crime list")
            } footer: {
                Text("The built-in charges can't be removed. The law is the law.")
            }

            Section {
                HStack {
                    Text("Longest streak to beat")
                    Spacer()
                    TextField("0", value: $seedLongestStreak, format: .number)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                        .frame(maxWidth: 100)
                    Text("days")
                        .foregroundStyle(.secondary)
                }
            } header: {
                Text("Your record before the app")
            } footer: {
                Text("Already know your longest clean run? Enter it and the app won't call a new personal best until you've beaten it. A challenge from past-you to future-you.")
            }

            Section {
                if streakStartTime > 0 {
                    let start = Date(timeIntervalSince1970: streakStartTime)
                    LabeledContent("Streak started", value: start.formatted(date: .abbreviated, time: .omitted))
                    if editingStreakStart {
                        DatePicker("Move it to", selection: $draftStreakStart, in: ...Date(), displayedComponents: .date)
                        Button("Move the start date", role: .destructive) {
                            confirmingStreakMove = true
                        }
                    } else {
                        Button("Change start date") {
                            draftStreakStart = start
                            editingStreakStart = true
                        }
                    }
                } else {
                    DatePicker("My streak started", selection: $draftStreakStart, in: ...Date(), displayedComponents: .date)
                    Button("Set my streak start") {
                        streakStartTime = draftStreakStart.timeIntervalSince1970
                        pushSnapshot()
                    }
                }
            } header: {
                Text("Already on a streak?")
            } footer: {
                Text(streakStartTime > 0
                     ? "Day 1 counts from here, not from install. Moving it is between you and your conscience."
                     : "Already sober? Set the date it began and the counter starts from there.")
            }

            Section("The morning after") {
                Stepper("Debrief at \(morningAfterHour):00", value: $morningAfterHour, in: 5...14)
            }

            Section {
                Toggle("Lock app with Face ID", isOn: $appLockEnabled)
            } footer: {
                Text("Your videos never leave this phone: not iCloud, not backups, not anywhere. The lock keeps over-the-shoulder eyes out too.")
            }

            Section("Help") {
                Button {
                    replayOnboarding = true
                } label: {
                    Label("How it works", systemImage: "sparkles")
                }
                Link(destination: URL(string: "https://pettifordo.github.io/GreenMonkeys/support.html") ?? URL(fileURLWithPath: "/")) {
                    Label("Help & support", systemImage: "questionmark.circle")
                }
                Link(destination: URL(string: "https://pettifordo.github.io/GreenMonkeys/privacy.html") ?? URL(fileURLWithPath: "/")) {
                    Label("Privacy policy", systemImage: "hand.raised")
                }
            }

            Section {
                Link("If this feels bigger than morning-after regret…",
                     destination: URL(string: "https://www.nhs.uk/live-well/alcohol-advice/") ?? URL(fileURLWithPath: "/"))
            } footer: {
                Text("Green Monkeys is a self-awareness tool, not treatment. If drinking is worrying you, the NHS page above is a good, judgement-free start.")
            }
        }
        .navigationTitle("Settings")
        .onChange(of: insultWord) { _, newWord in
            // Keep the widget's wording in step without waiting for a Home visit.
            var snapshot = StreakSnapshot.load()
            snapshot.insultWord = newWord
            snapshot.save()
            WidgetCenter.shared.reloadAllTimelines()
        }
        .alert("Move your streak start?", isPresented: $confirmingStreakMove) {
            Button("Move it anyway", role: .destructive) {
                streakStartTime = draftStreakStart.timeIntervalSince1970
                streakStartEdits += 1
                editingStreakStart = false
                pushSnapshot()
            }
            Button("Leave it", role: .cancel) {}
        } message: {
            Text(CharacterVoice.streakStartMoveWarning(edits: streakStartEdits))
        }
        .fullScreenCover(isPresented: $replayOnboarding) {
            // Replay never re-prompts for notifications.
            OnboardingView { _ in replayOnboarding = false }
        }
    }

    /// Recompute the widget snapshot's anchor after a streak-start change.
    private func pushSnapshot() {
        var snapshot = StreakSnapshot.load()
        snapshot.firstUseDate = AppSettings.streakAnchorDate
        snapshot.save()
        WidgetCenter.shared.reloadAllTimelines()
    }
}
