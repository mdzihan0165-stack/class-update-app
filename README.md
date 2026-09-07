<div align="center">

# 🚀 Kotlin Multiplatform Full-Stack Project

**One Codebase. Five Platforms.**  
Build, run, and share logic seamlessly across **Android**, **iOS**, **Web**, **Desktop**, and **Server**.

---

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#-running-the-apps)
[![iOS](https://img.shields.io/badge/Platform-iOS-000000?style=for-the-badge&logo=apple&logoColor=white)](#-running-the-apps)
[![Desktop](https://img.shields.io/badge/Platform-Desktop_(JVM)-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](#-running-the-apps)
[![Web](https://img.shields.io/badge/Platform-Web_(Wasm_&_JS)-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)](#-running-the-apps)
[![Server](https://img.shields.io/badge/Backend-Ktor_Server-E04E5F?style=for-the-badge&logo=ktor&logoColor=white)](#-running-the-apps)

</div>

---

## 🗺️ Project Architecture (Where Does Code Go?)

```plaintext
root/
├── 📱 app/
│   ├── iosApp/              # 🍏 iOS Entry Point (Xcode project & native SwiftUI code)
│   ├── androidApp/          # 🤖 Android App runner
│   ├── desktopApp/          # 🖥️ Desktop App runner
│   ├── webApp/              # 🌐 Web App runner
│   └── shared/              # 🎨 Shared Compose Multiplatform UI & Client Logic
│       ├── commonMain/      # 🌍 Shared code for ALL client platforms
│       ├── iosMain/         # 🍎 iOS-only code (e.g., Apple's CoreCrypto)
│       ├── jvmMain/         # 💻 Desktop-only code (JVM specific logic)
│       └── ...              # 🔧 Other platform-specific folders
│
├── 🧠 core/                 # ⚡ Core Business Logic (Shared by CLIENTS + SERVER)
│   ├── commonMain/          # 📦 Universal models & data contracts (Main hub!)
│   └── ...                  # 🔌 Optional platform-specific logic
│
└── ⚙️ server/               # 🚀 Backend Application (Powered by Ktor)
