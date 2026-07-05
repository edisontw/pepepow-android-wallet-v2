# Phase 4A Restore UI Status

Status: **Screen components ready / App wiring pending**

## Added

```text
src/screens/RestoreScreen.tsx
src/screens/SeedStartScreen.tsx
src/types/wallet.ts restore screen type
```

The restore component provides:

- 12-word restore input
- local phrase validation through `restoreValidation.ts`
- derived test address preview through `prototypeWalletDeriver`
- experimental restore warning
- restore and cancel callbacks

The seed start component provides:

- demo words display
- create/open test wallet button
- restore test wallet button
- warning copy for test-only use

## Current integration state

The building blocks are ready, but `App.tsx` has not yet been wired to them. Full-file updates to `App.tsx` have occasionally been blocked by connector safety checks, so this phase is proceeding by small files first.

## Next integration step

Wire:

```text
screen === "seed" -> SeedStartScreen
screen === "restore" -> RestoreScreen
RestoreScreen.onRestore(words) -> setWords(words), setWalletReady(true), setScreen("dashboard"), reset refresh cooldown
```

## Acceptance criteria for Phase 4A

- User can open restore mode from the seed screen.
- User can paste exactly 12 words.
- The app previews the derived test address locally.
- Restore sets wallet words locally.
- Dashboard refreshes balance/history/UTXO after restore.
- No recovery phrase is sent to the API.

## Safety boundary

This is still prototype restore only. Do not claim production compatibility until wallet-core derivation and restore scanning are reviewed.
