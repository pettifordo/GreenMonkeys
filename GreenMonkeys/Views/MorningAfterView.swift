import SwiftUI
import SwiftData
import WidgetKit
import PhotosUI

/// Receives a picked library video as a temp file we own (the picker's file
/// is only valid inside the import closure).
struct PickedMovie: Transferable {
    let url: URL

    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(contentType: .movie) { movie in
            SentTransferredFile(movie.url)
        } importing: { received in
            let destination = FileManager.default.temporaryDirectory
                .appendingPathComponent(UUID().uuidString + ".mov")
            try FileManager.default.copyItem(at: received.file, to: destination)
            return PickedMovie(url: destination)
        }
    }
}

/// The debrief: watch the evidence, judge each promise, confess the booze
/// crimes, and score yourself 0–5. Only the score is mandatory — but the flow
/// still ends pointing forward (SPEC hard rule 5).
struct MorningAfterView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(AppRouter.self) private var router
    let plan: SessionPlan

    @AppStorage(SettingsKey.insultWord) private var insultWord = "idiot"

    @State private var brokenFlags: [UUID: Bool] = [:]
    @State private var score: Int?
    @State private var selectedCrimes: Set<String> = []
    @State private var newCrime = ""
    @State private var oneChange = ""
    @State private var note = ""
    @State private var morningVideoFileName: String?
    @State private var showingCamera = false
    @State private var playingVideo: SessionVideo?
    @State private var crimeOptions: [String] = CatalogService.allCrimes
    @State private var pickedVideo: PhotosPickerItem?

    private var isJudged: Bool { plan.verdict != nil }

    /// Retro sessions (the confess flow) have no promises and no reminders —
    /// drunk-you COULDN'T have recorded in-app, so don't accuse.
    private var isUnplanned: Bool {
        plan.commitments.isEmpty && plan.reminderOffsetsMinutes.isEmpty
    }

    var body: some View {
        List {
            // The evidence leads: watch the tapes before you judge (owner request).
            if plan.video(.drunk) != nil || plan.video(.plan) != nil {
                Section("▶️ First, the evidence") {
                    if let drunk = plan.video(.drunk) {
                        Button {
                            playingVideo = drunk
                        } label: {
                            Label("Watch drunk you", systemImage: "play.circle.fill")
                        }
                    }
                    if let planVideo = plan.video(.plan) {
                        Button {
                            playingVideo = planVideo
                        } label: {
                            Label("Rewatch sober you's plan", systemImage: "play.circle")
                        }
                    }
                }
            }

            if isJudged {
                Section {
                    Button {
                        router.verdictPlan = plan
                    } label: {
                        Label("The verdict has been delivered — see the roast", systemImage: "hammer.fill")
                    }
                }
            } else {
                Section {
                    Text(CharacterVoice.monkeyMorningGreeting(
                        brutality: AppSettings.brutality,
                        word: insultWord
                    ))
                    .font(.headline)
                    Text(CharacterVoice.paranoiaMorningLine(brutality: AppSettings.brutality))
                        .foregroundStyle(.secondary)
                } header: {
                    Text("🐒🫣 The morning committee")
                }
                if plan.video(.drunk) == nil {
                    Section {
                        if !isUnplanned {
                            Text("No drunk video recorded. Suspiciously tidy, or too far gone to film?")
                                .foregroundStyle(.secondary)
                        }
                        PhotosPicker(selection: $pickedVideo, matching: .videos) {
                            Label(
                                isUnplanned ? "Add a video from last night" : "Import one from your library",
                                systemImage: "square.and.arrow.down"
                            )
                        }
                    } footer: {
                        if isUnplanned {
                            Text("Someone filmed it, didn't they. Add it to the evidence — imports stay on this phone like everything else.")
                        }
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
                ForEach(crimeOptions, id: \.self) { crime in
                    Toggle(isOn: bindingForCrime(crime)) {
                        Text(crime)
                    }
                    .disabled(isJudged)
                }
                if !isJudged {
                    HStack {
                        TextField("Add your own crime…", text: $newCrime)
                        Button("Confess") { addCustomCrime() }
                            .disabled(newCrime.trimmingCharacters(in: .whitespaces).isEmpty)
                    }
                }
            } header: {
                Text("The charge sheet")
            } footer: {
                Text("Anything you add is remembered for future mornings. The Monkeys keep excellent records.")
            }

            Section {
                HStack(spacing: 8) {
                    ForEach(0...5, id: \.self) { value in
                        Button {
                            score = value
                        } label: {
                            Text("\(value)")
                                .font(.headline)
                                .frame(maxWidth: .infinity, minHeight: 42)
                                .background(
                                    score == value
                                        ? (value == 0 ? Color.green : Color.red.opacity(0.35 + Double(value) * 0.13))
                                        : Color(.tertiarySystemFill),
                                    in: RoundedRectangle(cornerRadius: 8)
                                )
                                .foregroundStyle(score == value ? Color.white : Color.primary)
                        }
                        .buttonStyle(.plain)
                        .disabled(isJudged)
                    }
                }
                if let score {
                    Text(CharacterVoice.scoreGrading(score, word: insultWord))
                        .font(.subheadline.bold())
                }
                TextField("Anything Captain Paranoia should let go of?", text: $note, axis: .vertical)
                    .disabled(isJudged)
            } header: {
                Text("The verdict — how much of \(CharacterVoice.article(for: insultWord)) \(insultWord) were you?")
            } footer: {
                Text("The only mandatory question. Self-reported — the Monkeys will know if you lie. (They won't. But you will.)")
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
                TextField("Next time I will…", text: $oneChange, axis: .vertical)
                    .disabled(isJudged)
            } header: {
                Text("The one change (optional, but wise)")
            } footer: {
                Text("Shame fades by lunchtime; one concrete change doesn't.")
            }

            if !isJudged {
                Section {
                    Button {
                        saveVerdict()
                        router.verdictPlan = plan
                    } label: {
                        Text("Deliver the verdict")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(score == nil)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
                }
            }
        }
        .navigationTitle("The morning after")
        .onAppear { loadExisting() }
        .onChange(of: pickedVideo) { _, item in
            guard let item else { return }
            Task {
                if let movie = try? await item.loadTransferable(type: PickedMovie.self),
                   let fileName = try? VideoStore.shared.store(temporaryURL: movie.url) {
                    let video = SessionVideo(kind: .drunk, fileName: fileName)
                    video.plan = plan
                    plan.videos.append(video)
                    try? modelContext.save()
                }
                pickedVideo = nil
            }
        }
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

    private func bindingForBroken(_ commitment: Commitment) -> Binding<Bool> {
        Binding(
            get: { brokenFlags[commitment.id] ?? false },
            set: { brokenFlags[commitment.id] = $0 }
        )
    }

    private func bindingForCrime(_ crime: String) -> Binding<Bool> {
        Binding(
            get: { selectedCrimes.contains(crime) },
            set: { on in
                if on { selectedCrimes.insert(crime) } else { selectedCrimes.remove(crime) }
            }
        )
    }

    private func addCustomCrime() {
        let trimmed = newCrime.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return }
        CatalogService.rememberCrime(trimmed)
        crimeOptions = CatalogService.allCrimes
        // The typed text may differ in case from an existing entry — select the canonical one.
        let canonical = crimeOptions.first { $0.caseInsensitiveCompare(trimmed) == .orderedSame } ?? trimmed
        selectedCrimes.insert(canonical)
        newCrime = ""
    }

    private func loadExisting() {
        for commitment in plan.commitments {
            brokenFlags[commitment.id] = commitment.wasBroken ?? false
        }
        if let verdict = plan.verdict {
            score = verdict.effectiveScore
            selectedCrimes = Set(verdict.crimes)
            // Show this session's crimes even if they were later removed from the catalog.
            crimeOptions = CatalogService.allCrimes
            for crime in verdict.crimes where !crimeOptions.contains(crime) {
                crimeOptions.append(crime)
            }
            oneChange = verdict.oneChange
            note = verdict.note
        }
    }

    private func saveVerdict() {
        guard let score else { return }

        for commitment in plan.commitments {
            commitment.wasBroken = brokenFlags[commitment.id] ?? false
        }

        let verdict = Verdict(score: score, crimes: Array(selectedCrimes).sorted(), oneChange: oneChange, note: note)
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
        snapshot.insultWord = insultWord
        if score > 0 {
            snapshot.hasIdiotHistory = true
            if snapshot.anchorDate.map({ plan.sessionStart > $0 }) ?? true {
                snapshot.anchorDate = plan.sessionStart
            }
        }
        let allPlans = (try? modelContext.fetch(FetchDescriptor<SessionPlan>())) ?? []
        let idiotDates = allPlans
            .filter { ($0.verdict?.effectiveScore ?? 0) > 0 }
            .map(\.sessionStart)
        snapshot.longestStreak = StreakService.longestStreak(idiotDates: idiotDates, firstUseDate: AppSettings.streakAnchorDate)
        snapshot.save()
        WidgetCenter.shared.reloadAllTimelines()
        // The roast section renders now that plan.verdict exists.
    }
}
