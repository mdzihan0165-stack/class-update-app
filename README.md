# 🌐 Full-Stack Kotlin Multiplatform Workspace

> Production-ready monorepo targeting **Android, iOS, Desktop (JVM), Web (Wasm & JS)** with **Compose Multiplatform**, powered by a unified **Ktor** backend server.

---

## 🧭 Project Directory Layout

```plaintext
root/
├── app/
│   ├── iosApp/              # Xcode entry point & SwiftUI host for iOS
│   ├── androidApp/          # Android launcher & native AndroidManifest
│   ├── desktopApp/          # Desktop (JVM) launcher & windowing
│   ├── webApp/              # Browser runners (Wasm/Canvas & JS output)
│   └── shared/              # Shared UI layer (Compose Multiplatform)
│       ├── commonMain/      # Universal UI, ViewModels, cross-platform components
│       ├── iosMain/         # Native Apple platform code (e.g. CoreCrypto)
│       └── jvmMain/         # Desktop-specific platform overrides
├── core/                    # Headless domain layer (Shared across clients + server)
│   └── commonMain/          # DTOs, network models, business validation rules
└── server/                  # Ktor backend application
