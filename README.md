# PEPEW Wallet (Android Studio Kotlin Project Template)

A native Kotlin + Jetpack Compose Android project for the **PEPEW Wallet** prototype.

* **Package ID:** `net.pepepow.wallet`
* **Current Status:** **Phase 2 Read-only API Baseline**

---

## ⚠️ Important Security Warning

**This version is still a prototype. Phase 2 adds read-only PEPEW Light API lookups only.**

* It **does not** implement real 12-word seed/mnemonic generation.
* It **does not** store real mnemonic data.
* It **does not** implement private key derivation.
* It **does not** implement real address derivation.
* It **does not** implement transaction signing.
* It **does not** implement UTXO selection.
* It **does not** implement real transaction broadcasting.
* It only calls public read endpoints for health/status/address balance/history.
* No private keys or real wallet secrets are handled or transmitted.

---

## Tech Stack

* **Kotlin** (1.8.10)
* **Jetpack Compose** (Compose BOM 2023.08.00)
* **Material 3 UI Elements**
* **Navigation Compose**
* **ViewModel Layer**
* **Repository Pattern** with both `FakeWalletRepository` and active `ReadOnlyApiWalletRepository`

---

## Architecture

The Android source code resides inside `app/src/main/java/net/pepepow/wallet/`:

* **`ui/`**: Compose screens and app theme.
* **`viewmodel/`**: `WalletViewModel`, `SendViewModel`, `HistoryViewModel`, and `ApiStatusViewModel`.
* **`data/`**: `WalletRepository`, `FakeWalletRepository`, `ReadOnlyApiWalletRepository`, and `PepewApiClient`.
* **`domain/`**: Pure helper logic.
* **`security/`**: Placeholder `EncryptedStorage`. It does not store real seed data in this prototype.
* **`navigation/`**: Route definitions and navigation graph.

---

## Phase 2 Read-only API Specifications

* **Endpoint:** `https://light.pepepow.net/`
* **Active runtime data source:** `ReadOnlyApiWalletRepository`
* **Demo read-only address:** `PRfbEeHAKKbz6Voz85WJudrJwTA3ZbHunb`
* **Balance behavior:** read-only API lookup by address
* **History behavior:** read-only API lookup by address
* **API status behavior:** real `GET /api/health` and `GET /api/status`
* **Send behavior:** disabled in active read-only API mode until Phase 3

---

## Current Active Data Source

`MainActivity` currently creates:

```kotlin
val repository = ReadOnlyApiWalletRepository(PepewApiClient())
```

`FakeWalletRepository` remains in the codebase for mock mode, previews, fallback testing, and Phase 1 comparison.

---

## Development Roadmap

1. **Phase 1:** Mock wallet UI, navigation, ViewModels, fake repository state, fake send, and placeholder API/security classes.
2. **Phase 2 (Current):** Read-only API integration for balance/history/status only. No seed, key, signing, UTXO, or broadcast logic.
3. **Phase 3:** Local transaction preparation/signing design. Private keys must stay local.
4. **Phase 4:** Restore wallet flow and address scanning.
5. **Phase 5:** Keystore/encrypted storage, PIN/biometric, auto-lock, audit, and release hardening.

See also:

* [`PHASE1_STATUS.md`](PHASE1_STATUS.md)
* [`TODO_PHASE2_API.md`](TODO_PHASE2_API.md)
* [`PHASE2_STATUS.md`](PHASE2_STATUS.md)

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
