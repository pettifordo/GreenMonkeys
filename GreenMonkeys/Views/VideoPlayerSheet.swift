import SwiftUI
import AVKit

struct VideoPlayerSheet: View {
    let fileName: String
    @State private var player: AVPlayer?

    var body: some View {
        Group {
            if let player {
                VideoPlayer(player: player)
                    .ignoresSafeArea()
                    .onAppear { player.play() }
                    .onDisappear { player.pause() }
            } else {
                ContentUnavailableView(
                    "Video missing",
                    systemImage: "video.slash",
                    description: Text("This recording is no longer on the device.")
                )
            }
        }
        .onAppear {
            if VideoStore.shared.exists(fileName: fileName) {
                player = AVPlayer(url: VideoStore.shared.url(for: fileName))
            }
        }
    }
}
