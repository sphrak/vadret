import SwiftUI
import UIKit
import VadretShared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var isNativeNavigationEnabled: Bool?

    var body: some View {
        switch isNativeNavigationEnabled {
        case .none:
            Color.clear
                .task {
                    let isEnabled = try? await NativeNavigationSettingKt.isNativeNavigationEnabled()
                    isNativeNavigationEnabled = isEnabled?.boolValue ?? false
                }
        case .some(true):
            if #available(iOS 26.0, *) {
                NativeNavigationView()
            } else {
                ComposeRootView()
            }
        case .some(false):
            ComposeRootView()
        }
    }
}

struct ComposeRootView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
