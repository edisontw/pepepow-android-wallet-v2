# PEPEW Wallet (Android Studio Kotlin Project Template)

A native Kotlin + Jetpack Compose Android project for the **PEPEW Wallet** Phase 1 prototype.

* **Package ID:** `net.pepepow.wallet`
* **Current Status:** **Phase 1 Mock Wallet Prototype**

---

## ⚠️ Important Security Warning

**This version is for Phase 1 prototyping and uses mock data only.**

* It **does not** implement real 12-word seed/mnemonic generation.
* It **does not** store real mnemonic data.
* It **does not** implement private key derivation.
* It **does not** implement real address derivation.
* It **does not** implement transaction signing.
* It **does not** implement UTXO selection.
* It **does not** implement real transaction broadcasting.
* It **does not** make real PEPEW Light API calls yet.
* No private keys or real cryptocurrency balances are handled or transmitted.

---

## Tech Stack

* **Kotlin** (1.8.10)
* **Jetpack Compose** (Compose BOM 2023.08.00)
* **Material 3 UI Elements**
* **Navigation Compose**
* **ViewModel Layer**
* **Repository Pattern** with `FakeWalletRepository` as the only active data source

---

## Architecture

The Android source code resides inside `app/src/main/java/net/pepepow/wallet/`:

* **`ui/`**: Compose screens and app theme.
* **`viewmodel/`**: `WalletViewModel`, `SendViewModel`, `HistoryViewModel`, and `ApiStatusViewModel`.
* **`data/`**: `WalletRepository`, active `FakeWalletRepository`, and placeholder `PepewApiClient`.
* **`domain/`**: Pure helper logic.
* **`security/`**: Placeholder `EncryptedStorage`. It does not store real seed data in Phase 1.
* **`navigation/`**: Route definitions and navigation graph.

---

## Phase 1 Mock Specifications

* **Active Wallet Address:** `PExamplePepepowAddress123456789`
* **Mock Balance:** `12345.6789 PEPEW`
* **Reserved Endpoint shown in UI:** `https://light.pepepow.net/`
* **Send behavior:** fake local pending transaction only
* **API status behavior:** simulated local state only

---

## Current Active Data Source

`MainActivity` directly creates:

```kotlin
val repository = FakeWalletRepository()
```

This is intentional for Phase 1. Do not replace it with a real API repository until Phase 2.

---

## Development Roadmap

1. **Phase 1 (Current):** Mock wallet UI, navigation, ViewModels, fake repository state, fake send, and placeholder API/security classes.
2. **Phase 2:** Read-only API integration for balance/history/status only. No seed, key, signing, UTXO, or broadcast logic.
3. **Phase 3:** Local transaction preparation/signing design. Private keys must stay local.
4. **Phase 4:** Restore wallet flow and address scanning.
5. **Phase 5:** Keystore/encrypted storage, PIN/biometric, auto-lock, audit, and release hardening.

See also:

* [`PHASE1_STATUS.md`](PHASE1_STATUS.md)
* [`TODO_PHASE2_API.md`](TODO_PHASE2_API.md)

---

## How to Run the Project in Android Studio

### Prerequisites

1. Download and install **Android Studio**.
2. Install Android SDK 34 or newer.
3. Use JDK 17.

### Import and Build

1. Open Android Studio.
2. Select the repository root folder.
3. Let Android Studio run Gradle Sync.
4. Select the `app` run configuration.
5. Build or run on an emulator/device.

Manual build command, if a Gradle wrapper is available locally:

```bash
./gradlew assembleDebug
```

If no wrapper is present, run the equivalent Gradle task from Android Studio or generate a wrapper locally.
