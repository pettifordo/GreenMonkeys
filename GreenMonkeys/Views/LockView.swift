import SwiftUI
import LocalAuthentication

struct LockView: View {
    let onUnlocked: () -> Void
    @State private var failed = false

    var body: some View {
        VStack(spacing: 20) {
            Text("🐒")
                .font(.system(size: 64))
            Text("Green Monkeys is locked")
                .font(.title3.bold())
            Text("What happens in here stays in here.")
                .foregroundStyle(.secondary)
            if failed {
                Button("Try again") { authenticate() }
                    .buttonStyle(.borderedProminent)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .onAppear { authenticate() }
    }

    private func authenticate() {
        let context = LAContext()
        var error: NSError?
        guard context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &error) else {
            // No passcode set on device — nothing to lock behind.
            onUnlocked()
            return
        }
        context.evaluatePolicy(.deviceOwnerAuthentication,
                               localizedReason: "Unlock your videos") { success, _ in
            Task { @MainActor in
                if success { onUnlocked() } else { failed = true }
            }
        }
    }
}
