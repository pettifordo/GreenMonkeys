import SwiftUI

/// First-run explainer: the concept, the characters, Owen's story, and the
/// option to start from an existing sober streak. Shown once (gated by
/// `SettingsKey.hasOnboarded`); re-openable from Settings as "How it works".
struct OnboardingView: View {
    /// Called when the user finishes or skips. `askNotifications` is true only
    /// on genuine first-run completion — the Settings replay shouldn't re-prompt.
    let onFinish: (_ askNotifications: Bool) -> Void

    @AppStorage(SettingsKey.insultWord) private var insultWord = "idiot"
    @AppStorage(SettingsKey.sessionNoun) private var sessionNoun = "Session"

    @State private var page = 0
    @State private var hasStreak = false
    @State private var streakStart = Calendar.current.date(byAdding: .day, value: -30, to: Date()) ?? Date()

    private let lastPage = 3

    var body: some View {
        VStack(spacing: 0) {
            TabView(selection: $page) {
                welcome.tag(0)
                howItWorks.tag(1)
                story.tag(2)
                streakSetup.tag(3)
            }
            .tabViewStyle(.page(indexDisplayMode: .always))
            .indexViewStyle(.page(backgroundDisplayMode: .always))

            controls
        }
        .background(Color(.systemGroupedBackground))
    }

    // MARK: - Pages

    private var welcome: some View {
        OnboardingPage(
            emoji: "🐒",
            title: "Meet the Green Monkeys",
            message: "The morning after a big night, two voices ride your shoulder — the Green Monkeys telling you what a\(article) \(insultWord) you were, and Captain Paranoia fretting about what you said.\n\nThis app puts them both to work *for* you, before and after."
        )
    }

    private var howItWorks: some View {
        VStack(spacing: 22) {
            Text("How it works")
                .font(.title.bold())
            stepRow("🎬", "The Plan", "Sober-you records the promises before you go out.")
            stepRow("🐒", "The Session", "The Monkeys check in during the night. One tap films drunk-you.")
            stepRow("🫣", "The Morning After", "Watch the evidence, confess, score yourself 0–5.")
            stepRow("📉", "The Pattern", "The record you can't argue with — and your streak.")
        }
        .padding(.horizontal, 32)
        .frame(maxHeight: .infinity)
    }

    private var story: some View {
        VStack(spacing: 18) {
            Spacer()
            Text("🕊️")
                .font(.system(size: 60))
            Text("Built by someone who needed it")
                .font(.title2.bold())
                .multilineTextAlignment(.center)
            Text("\"Every 2–3 months I'd do something I'd cringe at the next morning. I couldn't imagine life without booze. Nine years sober now, I built the app I wish I'd had.\"")
                .font(.body)
                .italic()
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
            Text("The honest goal: use the app so that one day you don't need it.")
                .font(.callout.weight(.medium))
                .multilineTextAlignment(.center)
            Link("Read Owen's full story", destination: URL(string: "https://pettifordo.github.io/GreenMonkeys/story.html") ?? URL(fileURLWithPath: "/"))
                .font(.callout)
            Spacer()
        }
        .padding(.horizontal, 32)
    }

    private var streakSetup: some View {
        VStack(spacing: 18) {
            Spacer()
            Text("🏁")
                .font(.system(size: 56))
            Text("Already on a streak?")
                .font(.title2.bold())
            Text("If you're already dry and want the Monkeys to help you keep going, start where you are — Day 1 counts from your date, not today.")
                .font(.callout)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            Toggle("I'm already sober", isOn: $hasStreak.animation())
                .padding(.horizontal, 4)

            if hasStreak {
                DatePicker("Streak started", selection: $streakStart, in: ...Date(), displayedComponents: .date)
                Text("That's \(daysClean) days clean. Respect.")
                    .font(.headline)
                    .foregroundStyle(.green)
                Text("You can set this once. Move it later and the Monkeys will notice.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
            Spacer()
        }
        .padding(.horizontal, 32)
    }

    // MARK: - Controls

    private var controls: some View {
        VStack(spacing: 10) {
            Button {
                if page < lastPage {
                    withAnimation { page += 1 }
                } else {
                    finish()
                }
            } label: {
                Text(page < lastPage ? "Next" : "Let's go")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)

            Button(page < lastPage ? "Skip" : " ") {
                finish()
            }
            .font(.subheadline)
            .disabled(page == lastPage)
            .opacity(page == lastPage ? 0 : 1)
        }
        .padding(.horizontal, 24)
        .padding(.bottom, 12)
    }

    // MARK: - Helpers

    private var article: String {
        CharacterVoice.article(for: insultWord) == "an" ? "n" : ""
    }

    private var daysClean: Int {
        max(0, Calendar.current.dateComponents([.day], from: Calendar.current.startOfDay(for: streakStart), to: Calendar.current.startOfDay(for: Date())).day ?? 0)
    }

    private func finish() {
        // Only set on first run — never silently overwrite an existing streak
        // on replay; moving it is the snark-gated job of Settings.
        if hasStreak, AppSettings.streakStartDate == nil {
            UserDefaults.standard.set(streakStart.timeIntervalSince1970, forKey: SettingsKey.streakStartTime)
        }
        UserDefaults.standard.set(true, forKey: SettingsKey.hasOnboarded)
        onFinish(true)
    }

    private func stepRow(_ emoji: String, _ title: String, _ detail: String) -> some View {
        HStack(alignment: .top, spacing: 14) {
            Text(emoji).font(.title)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.headline)
                Text(detail).font(.subheadline).foregroundStyle(.secondary)
            }
            Spacer()
        }
    }
}

private struct OnboardingPage: View {
    let emoji: String
    let title: String
    let message: String

    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            Text(emoji).font(.system(size: 72))
            Text(title)
                .font(.largeTitle.bold())
                .multilineTextAlignment(.center)
            Text(.init(message))
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
            Spacer()
        }
        .padding(.horizontal, 32)
    }
}
