import SwiftUI
import UIKit

/// Front-camera video recorder. Deliberately one-tap from wherever it's
/// launched — drunk-you won't navigate menus (SPEC §1.2).
struct CameraRecorderView: UIViewControllerRepresentable {
    @Environment(\.dismiss) private var dismiss
    let onRecorded: (URL) -> Void

    static var isCameraAvailable: Bool {
        UIImagePickerController.isSourceTypeAvailable(.camera)
    }

    func makeUIViewController(context: Context) -> UIViewController {
        guard Self.isCameraAvailable else {
            // Simulator or camera-restricted device: explain instead of crashing.
            let controller = UIHostingController(rootView: CameraUnavailableView { dismiss() })
            return controller
        }
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.mediaTypes = ["public.movie"]
        picker.cameraCaptureMode = .video
        picker.cameraDevice = .front
        picker.videoQuality = .typeMedium
        picker.videoMaximumDuration = 120
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: CameraRecorderView

        init(parent: CameraRecorderView) {
            self.parent = parent
        }

        func imagePickerController(_ picker: UIImagePickerController,
                                   didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            if let url = info[.mediaURL] as? URL {
                parent.onRecorded(url)
            }
            parent.dismiss()
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.dismiss()
        }
    }
}

private struct CameraUnavailableView: View {
    let onClose: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("📵")
                .font(.system(size: 56))
            Text("No camera available")
                .font(.headline)
            Text("Video recording needs a real device with a camera.")
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button("Close", action: onClose)
                .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}
