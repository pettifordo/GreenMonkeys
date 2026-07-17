import SwiftUI
import SwiftData

/// Where sober-you makes the promises. Commitments + reminder times + the plan
/// video, all scheduled up-front (SPEC §1.1, §4).
struct PlanEditorView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Query private var allPlans: [SessionPlan]

    @State private var occasion = ""
    @State private var sessionStart = Calendar.current.date(bySettingHour: 19, minute: 0, second: 0, of: Date()) ?? Date()
    @State private var plannedEnd = Calendar.current.date(bySettingHour: 23, minute: 0, second: 0, of: Date()) ?? Date()
    @State private var drafts: [DraftCommitment] = [
        DraftCommitment(kind: .maxDrinks, detail: "4"),
        DraftCommitment(kind: .noDriving, detail: ""),
    ]
    @State private var reminderOffsets: Set<Int> = [60, 120, 180]
    @State private var planVideoFileName: String?
    @State private var showingCamera = false

    struct DraftCommitment: Identifiable {
        let id = UUID()
        var kind: CommitmentKind
        var detail: String
    }

    private static let reminderChoices = [30, 60, 90, 120, 180, 240]

    /// Every judged commitment across history, for the "you've promised this before" callbacks.
    private var judgedRecords: [CommitmentRecord] {
        allPlans
            .filter { $0.verdict != nil }
            .flatMap(\.commitments)
            .compactMap { commitment in
                commitment.wasBroken.map {
                    CommitmentRecord(key: commitment.patternKey, label: commitment.patternLabel, wasBroken: $0)
                }
            }
    }

    var body: some View {
        Form {
            Section("The occasion") {
                TextField("e.g. Friday pub, Dave's 40th", text: $occasion)
                DatePicker("Starts", selection: $sessionStart)
                DatePicker("Leaving at", selection: $plannedEnd)
            }

            Section {
                ForEach($drafts) { $draft in
                    CommitmentEditorRow(draft: $draft)
                }
                .onDelete { drafts.remove(atOffsets: $0) }

                Menu {
                    ForEach(availableKinds, id: \.self) { kind in
                        Button("\(kind.symbol) \(kind.label)") { add(kind) }
                    }
                    let savedCustoms = availableSavedCustoms
                    if !savedCustoms.isEmpty {
                        Divider()
                        ForEach(savedCustoms, id: \.self) { text in
                            Button("🤙 \(text)") {
                                drafts.append(DraftCommitment(kind: .custom, detail: text))
                            }
                        }
                    }
                    Divider()
                    Button("✍️ New promise…") {
                        drafts.append(DraftCommitment(kind: .custom, detail: ""))
                    }
                } label: {
                    Label("Add a promise", systemImage: "plus")
                }
            } header: {
                Text("Sober-you's promises")
            } footer: {
                if let callback = patternCallbacks.first {
                    Text(callback)
                        .foregroundStyle(.orange)
                }
            }

            Section {
                ForEach(Self.reminderChoices, id: \.self) { minutes in
                    Toggle(isOn: Binding(
                        get: { reminderOffsets.contains(minutes) },
                        set: { on in
                            if on { reminderOffsets.insert(minutes) } else { reminderOffsets.remove(minutes) }
                        }
                    )) {
                        Text(reminderLabel(minutes))
                    }
                }
            } header: {
                Text("Monkey check-ins during the session")
            } footer: {
                Text("Each check-in resurfaces the plan on your phone and Watch, with one tap to record drunk-you.")
            }

            Section {
                if planVideoFileName != nil {
                    Label("Plan video recorded", systemImage: "checkmark.circle.fill")
                        .foregroundStyle(.green)
                    Button("Re-record", role: .destructive) { showingCamera = true }
                } else {
                    Button {
                        showingCamera = true
                    } label: {
                        Label("Record your plan video", systemImage: "video.fill")
                    }
                }
            } header: {
                Text("Message from sober you")
            } footer: {
                Text("30 seconds of sober you saying exactly what the plan is. Tomorrow-you will thank you. Or wince.")
            }

            // Confirm actions live at the bottom, like everywhere else in the app.
            Section {
                Button {
                    save()
                } label: {
                    Text("Save the plan")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(drafts.isEmpty)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())

                Button {
                    if let fileName = planVideoFileName {
                        VideoStore.shared.delete(fileName: fileName)
                    }
                    dismiss()
                } label: {
                    Text("Cancel")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
                .padding(.top, 4)
            }
        }
        .navigationTitle("New \(AppSettings.sessionNoun)")
        .fullScreenCover(isPresented: $showingCamera) {
            CameraRecorderView { url in
                if let old = planVideoFileName {
                    VideoStore.shared.delete(fileName: old)
                }
                planVideoFileName = try? VideoStore.shared.store(temporaryURL: url)
            }
        }
    }

    // MARK: - Helpers

    private var availableKinds: [CommitmentKind] {
        CommitmentKind.allCases.filter { kind in
            kind != .custom && !drafts.contains { $0.kind == kind }
        }
    }

    /// Previously used custom promises (the user's own list), minus ones already drafted.
    private var availableSavedCustoms: [String] {
        CatalogService.customPromises.filter { text in
            !drafts.contains { $0.kind == .custom && $0.detail.caseInsensitiveCompare(text) == .orderedSame }
        }
    }

    private var patternCallbacks: [String] {
        drafts.compactMap { draft in
            let key = draft.kind == .custom
                ? draft.detail.trimmingCharacters(in: .whitespaces).lowercased()
                : draft.kind.rawValue
            guard !key.isEmpty else { return nil }
            let history = PatternService.history(forKey: key, in: judgedRecords)
            return CharacterVoice.patternCallback(
                label: draft.kind == .custom ? draft.detail : draft.kind.label,
                timesPromised: history.timesPromised,
                timesBroken: history.timesBroken,
                brutality: AppSettings.brutality,
                word: AppSettings.insultWord
            )
        }
    }

    private func add(_ kind: CommitmentKind) {
        let defaultDetail: String
        switch kind {
        case .maxDrinks: defaultDetail = "4"
        case .leaveBy:   defaultDetail = plannedEnd.formatted(date: .omitted, time: .shortened)
        default:         defaultDetail = ""
        }
        drafts.append(DraftCommitment(kind: kind, detail: defaultDetail))
    }

    private func reminderLabel(_ minutes: Int) -> String {
        let fire = sessionStart.addingTimeInterval(TimeInterval(minutes * 60))
        let hours = minutes % 60 == 0 ? "\(minutes / 60)h" : "\(minutes)m"
        return "\(hours) in — around \(fire.formatted(date: .omitted, time: .shortened))"
    }

    private func save() {
        let plan = SessionPlan(
            occasion: occasion,
            sessionStart: sessionStart,
            plannedEnd: plannedEnd,
            reminderOffsetsMinutes: reminderOffsets.sorted()
        )
        modelContext.insert(plan)

        for draft in drafts {
            let commitment = Commitment(kind: draft.kind, detail: draft.detail)
            commitment.plan = plan
            plan.commitments.append(commitment)
            // Custom promises join the catalog for future plans.
            if draft.kind == .custom {
                CatalogService.rememberPromise(draft.detail)
            }
        }

        if let fileName = planVideoFileName {
            let video = SessionVideo(kind: .plan, fileName: fileName)
            video.plan = plan
            plan.videos.append(video)
        }

        try? modelContext.save()

        let summary = plan.commitments.map(\.displayText).joined(separator: " · ")
        Task {
            await NotificationService.shared.schedule(
                planID: plan.id,
                occasion: plan.occasion,
                sessionStart: plan.sessionStart,
                plannedEnd: plan.plannedEnd,
                reminderOffsetsMinutes: plan.reminderOffsetsMinutes,
                commitmentsSummary: summary
            )
        }
        dismiss()
    }
}

// MARK: - CommitmentEditorRow

struct CommitmentEditorRow: View {
    @Binding var draft: PlanEditorView.DraftCommitment

    var body: some View {
        HStack {
            Text(draft.kind.symbol)
            switch draft.kind {
            case .maxDrinks:
                Stepper(value: Binding(
                    get: { Int(draft.detail) ?? 4 },
                    set: { draft.detail = String($0) }
                ), in: 0...20) {
                    Text("Max \(Int(draft.detail) ?? 4) drinks")
                }
            case .leaveBy:
                Text("Leave by")
                TextField("23:00", text: $draft.detail)
                    .multilineTextAlignment(.trailing)
            case .custom:
                TextField("Your promise…", text: $draft.detail)
            default:
                Text(draft.kind.label)
            }
        }
    }
}
