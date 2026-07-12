import SwiftUI

struct WatchContentView: View {
    @Environment(WatchConnectivityReceiver.self) private var receiver

    var body: some View {
        ScrollView {
            VStack(spacing: 10) {
                if let context = receiver.context {
                    Text("Days since you were a \(context.insultWord)")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                    Text("\(context.streakDays)")
                        .font(.system(size: 46, weight: .black, design: .rounded))
                        .foregroundStyle(context.streakDays == 0 ? .red : .green)

                    if let occasion = context.sessionOccasion {
                        Divider()
                        Text("🍻 \(occasion)")
                            .font(.headline)
                        if let start = context.sessionStart {
                            Text(start.formatted(date: .omitted, time: .shortened))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        ForEach(context.commitments, id: \.self) { commitment in
                            Text(commitment)
                                .font(.caption)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }
                } else {
                    Text("🐒")
                        .font(.system(size: 40))
                    Text("Open Green Monkeys on your iPhone to sync.")
                        .font(.caption)
                        .multilineTextAlignment(.center)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.horizontal, 4)
        }
        .navigationTitle("Monkeys")
    }
}
