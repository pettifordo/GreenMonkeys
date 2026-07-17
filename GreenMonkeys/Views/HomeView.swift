import SwiftUI
import SwiftData
import WidgetKit

/// Full-screen destinations the screenshot rig can open directly (debug builds).
enum ScreenshotCover: Identifiable {
    case session(SessionPlan)
    case morning(SessionPlan)
    case verdict(SessionPlan)
    case pattern
    case settings

    var id: String {
        switch self {
        case .session:  return "session"
        case .morning:  return "morning"
        case .verdict:  return "verdict"
        case .pattern:  return "pattern"
        case .settings: return "settings"
        }
    }
}

/// Value-based routes for HomeView's stack, so a path reset can unwind them.
enum HomeRoute: Hashable {
    case unplanned
    case debrief(SessionPlan)
}

struct HomeView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(AppRouter.self) private var router
    @Query(sort: \SessionPlan.sessionStart) private var plans: [SessionPlan]
    @State private var showingEditor = false
    @State private var path = NavigationPath()
    @State private var screenshotCover: ScreenshotCover?

    // @AppStorage so the home screen updates the moment Settings changes them.
    @AppStorage(SettingsKey.insultWord) private var insultWord = "idiot"
    @AppStorage(SettingsKey.sessionNoun) private var sessionNoun = "Session"
    @AppStorage(SettingsKey.seedLongestStreak) private var seedLongestStreak = 0

    @State private var planToCancel: SessionPlan?

    private var hasIdiotHistory: Bool {
        plans.contains { ($0.verdict?.effectiveScore ?? 0) > 0 }
    }

    private var streakDays: Int {
        let idiotDates = plans
            .filter { ($0.verdict?.effectiveScore ?? 0) > 0 }
            .map(\.sessionStart)
        return StreakService.daysSince(
            lastIdiotDate: StreakService.lastIdiotDate(idiotVerdictSessionStarts: idiotDates),
            firstUseDate: AppSettings.firstUseDate
        )
    }

    private var longestStreak: Int {
        max(
            StreakService.longestStreak(
                idiotDates: plans.filter { ($0.verdict?.effectiveScore ?? 0) > 0 }.map(\.sessionStart),
                firstUseDate: AppSettings.firstUseDate
            ),
            seedLongestStreak
        )
    }

    private var activePlan: SessionPlan? {
        plans.first { $0.status() == .active }
    }

    private var awaitingVerdict: [SessionPlan] {
        plans.filter { $0.status() == .awaitingVerdict }
    }

    private var upcoming: [SessionPlan] {
        plans.filter { $0.status() == .planned }
    }

    var body: some View {
        NavigationStack(path: $path) {
            List {
                streakSection

                if let plan = activePlan {
                    Section("Session in progress") {
                        NavigationLink(value: plan) {
                            PlanRow(plan: plan, badge: "🍻 Live")
                        }
                        .swipeActions(edge: .trailing) {
                            Button(role: .destructive) {
                                planToCancel = plan
                            } label: {
                                Label("Cancel", systemImage: "xmark.bin")
                            }
                        }
                    }
                }

                if !awaitingVerdict.isEmpty {
                    Section("Time to face the music") {
                        ForEach(awaitingVerdict) { plan in
                            NavigationLink(value: plan) {
                                PlanRow(plan: plan, badge: "🫣 Debrief due")
                            }
                            .swipeActions(edge: .trailing) {
                                Button(role: .destructive) {
                                    planToCancel = plan
                                } label: {
                                    Label("Delete", systemImage: "xmark.bin")
                                }
                            }
                        }
                    }
                }

                Section("Coming up") {
                    if upcoming.isEmpty {
                        Button {
                            showingEditor = true
                        } label: {
                            Text("No \(sessionNoun) planned. The Monkeys rest… for now. Tap to fix that.")
                                .foregroundStyle(.secondary)
                        }
                    }
                    ForEach(upcoming) { plan in
                        NavigationLink(value: plan) {
                            PlanRow(plan: plan, badge: nil)
                        }
                        .swipeActions(edge: .trailing) {
                            Button(role: .destructive) {
                                planToCancel = plan
                            } label: {
                                Label("Cancel", systemImage: "xmark.bin")
                            }
                        }
                    }
                }

                Section {
                    NavigationLink(value: HomeRoute.unplanned) {
                        Label("Rough morning, no plan? Confess", systemImage: "sunrise")
                    }
                } footer: {
                    Text("Last night got away from you without a plan? The debrief still counts.")
                }
            }
            .navigationTitle("Green Monkeys")
            .navigationDestination(for: SessionPlan.self) { plan in
                switch plan.status() {
                case .planned:                    PlanDetailView(plan: plan)
                case .active:                     SessionLiveView(plan: plan)
                case .awaitingVerdict:            MorningAfterView(plan: plan)
                case .completed:                  PlanDetailView(plan: plan)
                }
            }
            .navigationDestination(for: HomeRoute.self) { route in
                switch route {
                case .unplanned:               UnplannedDebriefView(path: $path)
                case .debrief(let plan):       MorningAfterView(plan: plan)
                }
            }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    NavigationLink { HistoryView() } label: {
                        Image(systemName: "chart.bar.doc.horizontal")
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink { SettingsView() } label: {
                        Image(systemName: "gearshape")
                    }
                }
                ToolbarItem(placement: .bottomBar) {
                    Button {
                        showingEditor = true
                    } label: {
                        // Explicit HStack: bottom-bar Labels collapse to icon-only.
                        HStack {
                            Image(systemName: "plus.circle.fill")
                            Text("Plan a \(sessionNoun)")
                        }
                        .font(.headline)
                    }
                }
            }
            .sheet(isPresented: $showingEditor) {
                NavigationStack { PlanEditorView() }
            }
            .confirmationDialog(
                planToCancel?.status() == .planned ? "Cancel this \(sessionNoun)?" : "Delete this \(sessionNoun)?",
                isPresented: Binding(
                    get: { planToCancel != nil },
                    set: { if !$0 { planToCancel = nil } }
                ),
                titleVisibility: .visible,
                presenting: planToCancel
            ) { plan in
                Button(
                    plan.status() == .planned
                        ? "It was created by mistake — delete it"
                        : "Delete it — evidence and all",
                    role: .destructive
                ) {
                    cancelPlan(plan)
                }
                Button("Keep it", role: .cancel) {}
            } message: { plan in
                let name = plan.occasion.isEmpty ? "This one" : "\"\(plan.occasion)\""
                if plan.status() == .planned {
                    Text("Are you sure? \(name) goes quietly — but you know the Monkeys are still watching. They saw you plan it.")
                } else {
                    Text("Are you sure? Was it created by mistake, or did it just go badly? \(name) and its videos vanish — but the Monkeys are still watching, and deleting it won't delete the memory of it.")
                }
            }
            .onAppear {
                pushWatchContext()
                pushStreakSnapshot()
            }
            .onChange(of: router.goHomeSignal) { _, _ in
                // "Finish — go face the day": unwind everything back to Home.
                path = NavigationPath()
                showingEditor = false
                screenshotCover = nil
            }
            .fullScreenCover(item: $screenshotCover) { cover in
                NavigationStack {
                    switch cover {
                    case .session(let plan): SessionLiveView(plan: plan)
                    case .morning(let plan): MorningAfterView(plan: plan)
                    case .verdict(let plan): VerdictView(plan: plan)
                    case .pattern:           HistoryView()
                    case .settings:          SettingsView()
                    }
                }
            }
            .task {
                #if DEBUG
                guard ScreenshotRig.isActive else { return }
                try? await Task.sleep(nanoseconds: 400_000_000)
                ScreenshotRig.seedDemoDataIfNeeded(context: modelContext)
                try? await Task.sleep(nanoseconds: 300_000_000)
                routeForScreenshot()
                #endif
            }
        }
    }

    private var streakSection: some View {
        Section {
            // The whole score is a tap target into the patterns screen.
            NavigationLink {
                HistoryView()
            } label: {
                VStack(spacing: 6) {
                    Text("Days since you were \(CharacterVoice.article(for: insultWord)) \(insultWord)")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text("\(streakDays)")
                        .font(.system(size: 72, weight: .black, design: .rounded))
                        .foregroundStyle(streakDays == 0 && hasIdiotHistory ? .red : .green)
                    Text(streakTagline)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    Text(streakDays >= longestStreak && streakDays > 0
                         ? "🏆 Personal best — beat yesterday's you again tomorrow"
                         : "Longest streak: \(longestStreak) days")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
            }
        }
    }

    private var streakTagline: String {
        if streakDays == 0 && hasIdiotHistory {
            return "Rock bottom has a floor. Build on it."
        }
        if !hasIdiotHistory {
            return "Clean record so far. The Monkeys are watching. 🐒"
        }
        return "Keep it going. 🐒"
    }

    #if DEBUG
    private func routeForScreenshot() {
        guard let screen = ScreenshotRig.screen else { return }
        let descriptor = FetchDescriptor<SessionPlan>(sortBy: [SortDescriptor(\SessionPlan.sessionStart)])
        let all = (try? modelContext.fetch(descriptor)) ?? []
        switch screen {
        case "editor":
            showingEditor = true
        case "session":
            if let plan = all.last(where: { $0.verdict == nil && !$0.commitments.isEmpty }) {
                screenshotCover = .session(plan)
            }
        case "morning":
            if let plan = all.first(where: { $0.status() == .awaitingVerdict }) {
                screenshotCover = .morning(plan)
            }
        case "roast":
            // A judged, crime-heavy verdict so the roast + closer render.
            if let plan = all.first(where: { ($0.verdict?.crimes.count ?? 0) >= 2 }) {
                screenshotCover = .verdict(plan)
            }
        case "pattern":
            screenshotCover = .pattern
        case "settings":
            screenshotCover = .settings
        default:
            break
        }
    }
    #endif

    private func pushWatchContext() {
        let tonight = activePlan ?? upcoming.first
        let context = WatchContext(
            streakDays: streakDays,
            insultWord: insultWord,
            sessionOccasion: tonight?.occasion,
            sessionStart: tonight?.sessionStart,
            commitments: tonight?.commitments.map(\.displayText) ?? []
        )
        PhoneConnectivityService.shared.push(context)
    }

    private func pushStreakSnapshot() {
        let idiotDates = plans
            .filter { ($0.verdict?.effectiveScore ?? 0) > 0 }
            .map(\.sessionStart)
        StreakSnapshot(
            anchorDate: StreakService.lastIdiotDate(idiotVerdictSessionStarts: idiotDates),
            hasIdiotHistory: !idiotDates.isEmpty,
            firstUseDate: AppSettings.firstUseDate,
            insultWord: insultWord,
            longestStreak: longestStreak
        ).save()
        WidgetCenter.shared.reloadAllTimelines()
    }

    private func cancelPlan(_ plan: SessionPlan) {
        for video in plan.videos {
            VideoStore.shared.delete(fileName: video.fileName)
        }
        let planID = plan.id
        Task { await NotificationService.shared.cancel(planID: planID) }
        modelContext.delete(plan)
        try? modelContext.save()
    }
}

// MARK: - PlanRow

struct PlanRow: View {
    let plan: SessionPlan
    let badge: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(plan.occasion.isEmpty ? "A session" : plan.occasion)
                    .font(.headline)
                Spacer()
                if let badge {
                    Text(badge)
                        .font(.caption.bold())
                }
            }
            Text(plan.sessionStart.formatted(date: .abbreviated, time: .shortened))
                .font(.subheadline)
                .foregroundStyle(.secondary)
            if !plan.commitments.isEmpty {
                Text(plan.commitments.map { $0.kind.symbol }.joined(separator: " "))
                    .font(.subheadline)
            }
        }
    }
}
