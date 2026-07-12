import SwiftUI
import SwiftData
import WidgetKit

/// The debrief: watch the evidence, judge each promise, deliver the verdict,
/// and — always — end forward-looking (hard rule 5).
struct MorningAfterView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    let plan: SessionPlan

    @State private var brokenFlags: [UUID: Bool] = [:]
    @State private var wasIdiot = false
    @State private var oneChange = ""
    @State private var note = ""
    @State private var morningVideoFileName: String?
    @State private var showingCamera = false
    @State private var playingVideo: SessionVideo?
    @State private var savedLine: String?

    private var isJudged: Bool { plan.verdict != nil }

    var body: some View {
        List {
            Section {
                Text(CharacterVoice.monkeyMorningGreeting(
                    brutality: AppSettings.brutality,
                    word: AppSettings.insultWord
                ))
                .font(.headline)
                Text(CharacterVoice.paranoiaMorningLine(brutality: AppSettings.brutality))
                    .foregroundStyle(.secondary)
            } header: {
                Text("🐒🫣 The morning committee")
            }

            Section("The evidence") {
                if let drunk = plan.video(.drunk) {
                    Button {
                        playingVideo = drunk
                    } label: {
                        Label("Watch drunk you", systemImage: "play.circle.fill")
                    }
                } else {
                    Text("No drunk video recorded. Suspiciously tidy, or too far gone to film?")
                        .foregroundStyle(.secondary)
                }
                if let planVideo = plan.video(.plan) {
                    Button {
                        playingVideo = planVideo
                    } label: {
                        Label("Rewatch sober you's plan", systemImage: "play.circle")
                    }
                }
            }

            if !plan.commitments.isEmpty {
                Section("The promises — honestly now") {
                    ForEach(plan.commitments) { commitment in
                        Toggle(isOn: bindingForBroken(commitment)) {
                            VStack(alignment: .leading) {
                                Text(commitment.displayText)
                                Text(brokenFlags[commitment.id] == true ? "Broken" : "Kept")
                                    .font(.caption)
                                    .foregroundStyle(brokenFlags[commitment.id] == true ? .red : .green)
                            }
                        }
                        .disabled(isJudged)
                    }
                }
            }

            Section {
                Toggle("Were you a\(anSuffix) \(AppSettings.insultWord) last night?", isOn: $wasIdiot)
                    .disabled(isJudged)
                TextField("Anything Captain Paranoia should let go of?", text: $note, axis: .vertical)
                    .disabled(isJudged)
            } header: {
                Text("The verdict")
            } footer: {
                Text("Self-reported. The Monkeys will know if you lie. (They won't. But you will.)")
            }

            Section("Say it to camera") {
                if morningVideoFileName != nil || plan.video(.morningAfter) != nil {
                    Label("Morning-after video recorded", systemImage: "checkmark.circle.fill")
                        .foregroundStyle(.green)
                } else {
                    Button {
                        showingCamera = true
                    } label: {
                        Label("Record your morning-after video", systemImage: "video.fill")
                    }
                    .disabled(isJudged)
                }
            }

            Section {
                TextField("Next session I will…", text: $oneChange, axis: .vertical)
                    .disabled(isJudged)
            } header: {
                Text("The one change")
            } footer: {
                Text("The only mandatory part. Shame fades by lunchtime; one concrete change doesn't.")
            }

            if let savedLine {
                Section {
                    Text(savedLine)
                        .font(.headline)
                }
            }

            if !isJudged {
                Section {
                    Button {
                        saveVerdict()
                    } label: {
                        Text("Deliver the verdict")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(oneChange.trimmingCharacters(in: .whitespaces).isEmpty)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
                }
            }
        }
        .navigationTitle("The morning after")
        .onAppear { loadExisting() }
        .fullScreenCover(isPresented: $showingCamera) {
            CameraRecorderView { url in
                morningVideoFileName = try? VideoStore.shared.store(temporaryURL: url)
            }
        }
        .sheet(item: $playingVideo) { video in
            VideoPlayerSheet(fileName: video.fileName)
        }
    }

    // MARK: - Helpers

    private var anSuffix: String {
        let first = AppSettings.insultWord.lowercased().first
        return "aeiou".contains(first ?? " ") ? "n" : ""
    }

    private func bindingForBroken(_ commitment: Commitment) -> Binding<Bool> {
        Binding(
            get: { brokenFlags[commitment.id] ?? false },
            set: { brokenFlags[commitment.id] = $0 }
        )
    }

    private func loadExisting() {
        for commitment in plan.commitments {
            brokenFlags[commitment.id] = commitment.wasBroken ?? false
        }
        if let verdict = plan.verdict {
            wasIdiot = verdict.wasIdiot
            oneChange = verdict.oneChange
            note = verdict.note
        }
    }

    private func saveVerdict() {
        for commitment in plan.commitments {
            commitment.wasBroken = brokenFlags[commitment.id] ?? false
        }

        let verdict = Verdict(wasIdiot: wasIdiot, oneChange: oneChange, note: note)
        verdict.plan = plan
        plan.verdict = verdict

        if let fileName = morningVideoFileName {
            let video = SessionVideo(kind: .morningAfter, fileName: fileName)
            video.plan = plan
            plan.videos.append(video)
        }

        try? modelContext.save()

        Task { await NotificationService.shared.cancel(planID: plan.id) }

        // Keep the widget honest immediately — don't wait for a Home visit.
        var snapshot = StreakSnapshot.load()
        snapshot.insultWord = AppSettings.insultWord
        if wasIdiot {
            snapshot.hasIdiotHistory = true
            if snapshot.anchorDate.map({ plan.sessionStart > $0 }) ?? true {
                snapshot.anchorDate = plan.sessionStart
            }
        }
        snapshot.save()
        WidgetCenter.shared.reloadAllTimelines()

        savedLine = wasIdiot
            ? CharacterVoice.monkeyAfterIdiotVerdict(brutality: AppSettings.brutality, word: AppSettings.insultWord)
            : CharacterVoice.monkeyAfterCleanVerdict(brutality: AppSettings.brutality, word: AppSettings.insultWord)
    }
}
