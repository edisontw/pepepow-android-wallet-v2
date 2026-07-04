# PEPEW Wallet (Android Studio Kotlin Project Template)

A complete native Kotlin + Jetpack Compose Android project for the **PEPEW Wallet**, built following modern Android design guidelines and architectural best practices.

* **Package ID:** `net.pepepow.wallet`
* **Current Status:** **Phase 1 Mock Wallet Prototype**

---

## ⚠️ Important Security Warning
**This version is for Phase 1 prototyping and utilizes mock data only.**  
* It **does not** implement real 12-word seed/mnemonic generation.
* It **does not** store real mnemonic data.
* It **does not** implement private key derivation, transaction signing, UTXO selection, or transaction broadcasting.
* No private keys or real cryptocurrency balances are handled or transmitted.

---

## Tech Stack
* **Kotlin** (1.8.10)
* **Jetpack Compose** (Compose BOM 2023.08.00)
* **Material 3 UI Elements**
* **Navigation Compose** (Declarative route navigation)
* **ViewModel Layer** (Decoupled state management)
* **Repository Pattern** (`FakeWalletRepository` offline backend simulation)

---

## Architecture
The source code resides inside `app/src/main/java/net/pepepow/wallet/` and is divided into clean, decoupled modules:

* **`ui/`**: Layout screens (`Screens.kt`) and primary `MainActivity` built with elegant dark themes, space-optimized paddings, and standard Compose surface styles.
* **`viewmodel/`**: Dynamic viewmodels (`WalletViewModel.kt`, `SendViewModel.kt`, `HistoryViewModel.kt`, `ApiStatusViewModel.kt`) separating logic from ui layers.
* **`data/`**: Interface boundaries (`WalletRepository.kt`), active implementation state (`FakeWalletRepository.kt`), and Phase 2 node endpoints (`PepewApiClient.kt`).
* **`domain/`**: Pure domain helper classes (`WalletUseCases.kt`) for parsing validations and calculations.
* **`security/`**: Storage interface helpers (`EncryptedStorage.kt`) ready to be upgraded with hardware keys in Phase 2.
* **`navigation/`**: Central route definitions (`WalletRoutes.kt`) and graph state handlers (`WalletNavGraph.kt`).

---

## Phase 1 Mock Specifications
* **Active Wallet Address:** `PExamplePepepowAddress123456789`
* **Mock Balance:** `12345.6789 PEPEW`
* **Reserved Endpoint (Phase 2 Integration):** `https://light.pepepow.net/`

---

## Development Roadmap
1. **Phase 1 (Current):** Mock wallet with UI/UX transitions, offline database models, and mock state managers.
2. **Phase 2:** Read-Only API sync with public nodes to track addresses.
3. **Phase 3:** Integration of local cryptographic seed generation and standard key derivation.
4. **Phase 4:** Restore capabilities and secure transaction signing.
5. **Phase 5:** Hardware keystore integration, release audit, and play store publishing.

---

## How to Run the Project in Android Studio

### Prerequisites
1. Download and install **Android Studio** (Ladybug 2024.2.1 or newer recommended).
2. Android SDK 34 (Upside Down Cake) or newer configured.

### Import and Build
1. In AI Studio, click on **Settings (Gear Icon) -> Export to ZIP** to download the clean project structure.
2. Extract the downloaded ZIP archive to your local directory.
3. Open Android Studio, click on **Open**, and navigate to the extracted directory containing the project root.
4. Select the folder and click **OK**. Android Studio will import the project and automatically trigger a **Gradle Sync**.
5. Once the sync is complete, select **app** in the Run configurations dropdown.

### Running on Emulator or Physical Device
1. To run on an **Emulator**: Open the **Device Manager**, select or create an Android Virtual Device (AVD) running API level 26 (Android 8.0) or newer, and start it.
2. To run on a **Physical Device**: Enable **USB Debugging** on your device, connect it via USB or WiFi, and ensure it appears in the device dropdown list.
3. Click the **Run button (Green Play Icon)** or press `Shift + F10` (`Control + R` on macOS) to compile and install the APK on the device.
