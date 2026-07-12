import SwiftUI
import SwiftData

/// Read-only detail for planned and completed sessions, with delete.
struct PlanDetailView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    let plan: SessionPlan

    @State private var playingVideo: SessionVideo?
    @State private var confirmingDelete = false

    var body: some View {
        List {
            Section("When") {
                LabeledContent("Starts", value: plan.sessionStart.formatted(date: .abbreviated, time: .shortened))
                LabeledContent("Leaving at", value: plan.plannedEnd.formatted(date: .omitted, time: .shortened))
            }

            Section("The promises") {
                ForEach(plan.commitments) { commitment in
                    HStack {
                        Text(commitment.displayText)
                        Spacer()
                        if let broken = commitment.wasBroken {
                            Text(broken ? "Broken ✗" : "Kept ✓")
                                .font(.caption.bold())
                                .foregroundStyle(broken ? .red : .green)
                        }
                    }
                }
            }

            if let verdict = plan.verdict {
                Section("The verdict") {
                    LabeledContent("Were you one?", value: verdict.wasIdiot ? "Yes 🙈" : "No 🎉")
                    if !verdict.oneChange.isEmpty {
                        LabeledContent("One change") { Text(verdict.oneChange) }
                    }
                    if !verdict.note.isEmpty {
                        LabeledContent("Note") { Text(verdict.note) }
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
            }
        }
        .navigationTitle(plan.occasion.isEmpty ? "Session" : plan.occasion)
        .sheet(item: $playingVideo) { video in
            VideoPlayerSheet(fileName: video.fileName)
        }
        .confirmationDialog(
            "Delete this session and all its videos? This cannot be undone.",
            isPresented: $confirmingDelete,
            titleVisibility: .visible
        ) {
            Button("Delete everything", role: .destructive) { deletePlan() }
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
