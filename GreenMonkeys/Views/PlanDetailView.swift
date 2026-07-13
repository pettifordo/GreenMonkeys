import SwiftUI
import SwiftData

/// Detail for planned and completed sessions: the promises, the verdict, the
/// charge sheet, the videos — and a deliberately obstructive delete.
struct PlanDetailView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Environment(AppRouter.self) private var router
    let plan: SessionPlan

    @AppStorage(SettingsKey.insultWord) private var insultWord = "idiot"

    @State private var playingVideo: SessionVideo?
    @State private var confirmingDelete = false
    @State private var confirmingDeleteFinal = false

    var body: some View {
        List {
            Section("When") {
                LabeledContent("Starts", value: plan.sessionStart.formatted(date: .abbreviated, time: .shortened))
                LabeledContent("Leaving at", value: plan.plannedEnd.formatted(date: .omitted, time: .shortened))
            }

            if !plan.commitments.isEmpty {
                Section("The promises") {
                    ForEach(plan.commitments) { commitment in
                        HStack {
                            Text(commitment.displayText)
                            Spacer()
                            if let broken = commitment.wasBroken, plan.verdict != nil {
                                Text(broken ? "Broken ✗" : "Kept ✓")
                                    .font(.caption.bold())
                                    .foregroundStyle(broken ? .red : .green)
                            }
                        }
                    }
                }
            }

            if let verdict = plan.verdict {
                Section("The verdict") {
                    LabeledContent("Score", value: "\(verdict.effectiveScore) / 5")
                    Text(CharacterVoice.scoreGrading(verdict.effectiveScore, word: insultWord))
                        .font(.subheadline)
                        .foregroundStyle(verdict.effectiveScore == 0 ? .green : .red)
                    if !verdict.crimes.isEmpty {
                        LabeledContent("Charge sheet") {
                            Text(verdict.crimes.joined(separator: ", "))
                                .multilineTextAlignment(.trailing)
                        }
                    }
                    if !verdict.oneChange.isEmpty {
                        LabeledContent("One change") { Text(verdict.oneChange) }
                    }
                    if !verdict.note.isEmpty {
                        LabeledContent("Note") { Text(verdict.note) }
                    }
                    Button {
                        router.verdictPlan = plan
                    } label: {
                        Label("Relive the roast", systemImage: "flame")
                    }
                }
            } else {
                Section("Get on with it") {
                    NavigationLink {
                        SessionLiveView(plan: plan)
                    } label: {
                        Label("Open the session screen", systemImage: "party.popper")
                    }
                    NavigationLink {
                        MorningAfterView(plan: plan)
                    } label: {
                        Label("Record the outcome", systemImage: "sunrise")
                    }
                }
            }

            if !plan.videos.isEmpty {
                Section("Videos") {
                    ForEach(plan.videos) { video in
                        Button {
                            playingVideo = video
                        } label: {
                            Label(
                                "\(VideoKind(rawValue: video.kind)?.label ?? "Video") — \(video.recordedAt.formatted(date: .abbreviated, time: .shortened))",
                                systemImage: "play.circle"
                            )
                        }
                    }
                }
            }

            Section {
                Button("Delete this session", role: .destructive) {
                    confirmingDelete = true
                }
            } footer: {
                Text("Deleting is allowed. Forgetting is not guaranteed.")
            }
        }
        .navigationTitle(plan.occasion.isEmpty ? "Session" : plan.occasion)
        .sheet(item: $playingVideo) { video in
            VideoPlayerSheet(fileName: video.fileName)
        }
        .confirmationDialog(
            CharacterVoice.deleteWarnings.first,
            isPresented: $confirmingDelete,
            titleVisibility: .visible
        ) {
            Button("I accept the shame is permanent", role: .destructive) {
                confirmingDeleteFinal = true
            }
            Button("Fine, keep it", role: .cancel) {}
        }
        .alert("Absolutely sure?", isPresented: $confirmingDeleteFinal) {
            Button("Delete it all", role: .destructive) { deletePlan() }
            Button("No, I'll own it", role: .cancel) {}
        } message: {
            Text(CharacterVoice.deleteWarnings.second)
        }
    }

    private func deletePlan() {
        for video in plan.videos {
            VideoStore.shared.delete(fileName: video.fileName)
        }
        let planID = plan.id
        Task { await NotificationService.shared.cancel(planID: planID) }
        modelContext.delete(plan)
        try? modelContext.save()
        dismiss()
    }
}
