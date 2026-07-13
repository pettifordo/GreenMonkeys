import SwiftUI
import SwiftData

@main
struct GreenMonkeysApp: App {
    @State private var router = AppRouter()

    private let container: ModelContainer

    init() {
        let schema = Schema([SessionPlan.self, Commitment.self, SessionVideo.self, Verdict.self])
        do {
            container = try ModelContainer(for: schema)
        } catch {
            preconditionFailure("Failed to create SwiftData container: \(error)")
        }

        NotificationService.shared.configure()
        PhoneConnectivityService.shared.activate()
        _ = AppSettings.firstUseDate
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(router)
                .task {
                    #if DEBUG
                    if ScreenshotRig.isActive { return }
                    #endif
                    _ = await NotificationService.shared.requestAuthorization()
                }
                .onAppear {
                    let router = router
                    NotificationService.shared.onRecordRequested = { planID in
                        router.pendingAction = .recordDrunkVideo(planID: planID)
                    }
                    NotificationService.shared.onMorningAfterOpened = { planID in
                        router.pendingAction = .morningAfter(planID: planID)
                    }
                }
        }
        .modelContainer(container)
    }
}
