import SwiftUI
import SwiftData
import LocalAuthentication

/// Hosts the app behind the optional Face ID lock and presents flows requested
/// by notification taps (record drunk video / morning-after debrief).
struct RootView: View {
    @Environment(AppRouter.self) private var router
    @Environment(\.modelContext) private var modelContext
    @Environment(\.scenePhase) private var scenePhase
    @Query private var plans: [SessionPlan]

    @State private var unlocked: Bool = {
        #if DEBUG
        if ScreenshotRig.isActive { return true }
        #endif
        return !AppSettings.appLockEnabled
    }()
    @State private var recordingPlan: SessionPlan?
    @State private var debriefPlan: SessionPlan?

    var body: some View {
        @Bindable var router = router
        return ZStack {
            if unlocked {
                HomeView()
            } else {
                LockView(onUnlocked: { unlocked = true })
            }
        }
        .fullScreenCover(item: $router.verdictPlan, onDismiss: {
            router.consumePendingGoHome()
        }) { plan in
            NavigationStack { VerdictView(plan: plan) }
        }
        .onChange(of: router.goHomeSignal) { _, _ in
            recordingPlan = nil
            debriefPlan = nil
        }
        .onChange(of: router.pendingAction) { _, action in
            guard let action else { return }
            router.pendingAction = nil
            switch action {
            case .recordDrunkVideo(let planID):
                recordingPlan = plans.first { $0.id == planID }
            case .morningAfter(let planID):
                debriefPlan = plans.first { $0.id == planID }
            }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .background && AppSettings.appLockEnabled {
                unlocked = false
            }
        }
        .fullScreenCover(item: $recordingPlan) { plan in
            CameraRecorderView { url in
                saveVideo(from: url, kind: .drunk, plan: plan)
            }
        }
        .sheet(item: $debriefPlan) { plan in
            NavigationStack {
                MorningAfterView(plan: plan)
            }
        }
    }

    private func saveVideo(from url: URL, kind: VideoKind, plan: SessionPlan) {
        guard let fileName = try? VideoStore.shared.store(temporaryURL: url) else { return }
        let video = SessionVideo(kind: kind, fileName: fileName)
        video.plan = plan
        plan.videos.append(video)
        try? modelContext.save()
    }
}
