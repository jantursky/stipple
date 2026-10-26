import SwiftUI

@main
struct GalleryApp: App {
    var body: some Scene {
        WindowGroup {
            // Compose draws edge to edge and keeps its controls inside the safe area itself.
            ContentView()
                .ignoresSafeArea()
        }
    }
}
