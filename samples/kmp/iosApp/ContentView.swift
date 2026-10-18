// Minimal SwiftUI entry point for the Laboratory KMP sample.
//
// Copy this file into your Xcode project after running `pod install`. The one load-bearing
// line is the `InspectorHostKt.debugInspectorViewController()` call — everything else is a
// standard SwiftUI shell.

import SwiftUI
import UIKit
import LaboratorySample

struct ContentView: View {
    @State private var presenting = false

    var body: some View {
        VStack(spacing: 24) {
            Text("Laboratory sample")
                .font(.title)
            Button("Open Laboratory inspector") {
                presenting = true
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
        .fullScreenCover(isPresented: $presenting) {
            InspectorView()
                .ignoresSafeArea()
                .overlay(alignment: .topTrailing) {
                    Button("Done") { presenting = false }
                        .padding()
                }
        }
    }
}

/// Hosts the Kotlin `UIViewController` the SDK produces.
struct InspectorView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // `InspectorHost` is the Kotlin source file name; Kotlin top-level functions compile to
        // `<FileName>Kt` on the Swift side.
        InspectorHostKt.debugInspectorViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
