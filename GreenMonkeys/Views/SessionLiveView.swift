import SwiftUI
import SwiftData

/// The in-session screen: the plan, sober-you's video, and one big button to
/// record the evidence.
struct SessionLiveView: View {
    @Environment(\.modelContext) private var modelContext
    let plan: SessionPlan

    @State private var showingCamera = false
    @State private var playingVideo: SessionVideo?

    var body: some View {
        List {
            Section {
                Text(CharacterVoice.monkeySessionNudge(
                    brutality: AppSettings.brutality,
                    word: AppSettings.insultWord
                ))
                .font(.headline)
            } header: {
                Text("🐒 The Monkeys say")
            }

            Section("The plan") {
                ForEach(plan.commitments) { commitment in
                    Label(commitment.displayText, systemImage: "checkmark.seal")
                }
                if let planVideo = plan.video(.plan) {
                    Button {
                        playingVideo = planVideo
                    } label: {
                        Label("Watch sober you explain the plan", systemImage: "play.circle.fill")
                    }
                }
            }

            Section {
                Button {
                    showingCamera = true
                } label: {
                    Label("Record drunk-you 🎥", systemImage: "video.fill")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
            } footer: {
                Text("Tomorrow's you gets to watch this. Smile!")
            }

            if let drunk = plan.video(.drunk) {
                Section("Already in evidence") {
                    Button {
                        playingVideo = drunk
                    } label: {
                        Label("Recorded \(drunk.recordedAt.formatted(date: .omitted, time: .shortened))", systemImage: "film")
                    }
                }
            }
        }
        .navigationTitle(plan.occasion.isEmpty ? "Session" : plan.occasion)
        .fullScreenCover(isPresented: $showingCamera) {
            CameraRecorderView { url in
                saveDrunkVideo(from: url)
            }
        }
        .sheet(item: $playingVideo) { video in
            VideoPlayerSheet(fileName: video.fileName)
        }
    }

    private func saveDrunkVideo(from url: URL) {
        guard let fileName = try? VideoStore.shared.store(temporaryURL: url) else { return }
        // Keep every clip: never overwrite a recording (hard rule 3).
        let video = SessionVideo(kind: .drunk, fileName: fileName)
        video.plan = plan
        plan.videos.append(video)
        try? modelContext.save()
    }
}
