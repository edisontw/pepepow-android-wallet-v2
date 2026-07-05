# Phase 1 Baseline Status

Date: 2026-07-05
Repository: `edisontw/pepepow-android-wallet-v2`
App: PEPEW Wallet
Package: `net.pepepow.wallet`

## Scope

This repository is currently locked to a **Phase 1 mock wallet prototype**.

Phase 1 must remain UI/state/mock-data only:

- No real mnemonic generation.
- No seed persistence.
- No private key derivation.
- No address derivation.
- No real transaction signing.
- No UTXO selection.
- No real transaction broadcast.
- No real PEPEW Light API calls.

## Baseline Check

| Item | Status | Notes |
| --- | --- | --- |
| Valid Android Studio project | Pass | Root Gradle project includes `:app`; Android app module uses package/namespace `net.pepepow.wallet`. |
| Gradle sync readiness | Pass after minimal fixes | Added missing mock repository/domain source, Compose theme placeholder, launcher icon resources, and Compose layout opt-in needed by generated UI. |
| `assembleDebug` readiness | Expected pass | Repository files were corrected for known compile blockers. The connector environment cannot run Android Gradle because GitHub is accessed through the GitHub connector, not a local cloned Android SDK build environment. Run `./gradlew assembleDebug` or Android Studio Build > Make Project locally to confirm. |
| Only one Android `MainActivity` | Pass | One Android activity exists at `app/src/main/java/net/pepepow/wallet/MainActivity.kt`. The root `src/App.tsx` is an AI Studio simulator artifact and is not part of the Android app module. |
| Active data source | Pass | `MainActivity` directly instantiates `FakeWalletRepository()`. No real repository implementation is wired. |
| `FakeWalletRepository` | Pass | Added as the only active Phase 1 data source. It uses fixed mock address, balance, mnemonic text, history, API state, and fake pending-send behavior. |
| `PepewApiClient` | Pass | Placeholder only. It is not wired into `MainActivity`, ViewModels, or `FakeWalletRepository`. |
| `EncryptedStorage` | Pass | Placeholder only. It does not store or return real mnemonic data. |
| README Phase 1 marking | Pass | README already marks current status as Phase 1 mock prototype and warns that real mnemonic/private key/signing/UTXO/broadcast are not implemented. |

## Files Added or Corrected

- `app/src/main/java/net/pepepow/wallet/data/WalletRepository.kt`
  - `WalletRepository`
  - `FakeWalletRepository`
  - `Transaction`
  - `ApiState`
- `app/src/main/java/net/pepepow/wallet/ui/theme/Theme.kt`
- `app/src/main/java/net/pepepow/wallet/ui/screens/CompileAliases.kt`
- `app/src/main/res/drawable/ic_launcher.xml`
- `app/src/main/res/drawable/ic_launcher_round.xml`
- `PHASE1_STATUS.md`
- `TODO_PHASE2_API.md`

## Minimal Compile Fixes Applied

These were compile/support fixes only, not behavior redesigns:

1. Added missing source files required by existing imports.
2. Added placeholder theme function required by `MainActivity`.
3. Added placeholder launcher icon resources and updated manifest references.
4. Added Compose layout opt-in for generated `FlowRow` usage.
5. Added compatibility shims for generated UI naming issues:
   - `Alignment.CenterHorizer` typo shim.
   - `ActionButton(icon = ...)` overload.

## Current Phase 1 Mock Values

- Address: `PExamplePepepowAddress123456789`
- Balance: `12345.6789 PEPEW`
- Endpoint shown in UI: `https://light.pepepow.net/`
- API state: simulated only
- Send action: fake local pending transaction only

## Do Not Add in Phase 1

Do not add any of the following until the proper phase:

- BIP39 or other mnemonic generation.
- Seed encryption or keystore persistence.
- Private key derivation.
- PEPEW address derivation.
- UTXO retrieval or selection.
- Fee/change calculation using live chain data.
- Raw transaction construction or signing.
- Broadcast calls.
- Live balance/history/API calls.
