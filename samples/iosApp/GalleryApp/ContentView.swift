import SwiftUI
import GalleryKit

/// The Compose gallery. Launch arguments `-entry <id>` and `-dark NO` open a page and theme directly.
struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
