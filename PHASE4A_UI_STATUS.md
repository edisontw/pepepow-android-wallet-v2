# Phase 4A Restore UI Status

Status: **UI component scaffold added**

## Added

```text
src/screens/RestoreScreen.tsx
```

The new component provides:

- 12-word restore input
- local phrase validation through `restoreValidation.ts`
- derived test address preview through `prototypeWalletDeriver`
- experimental restore warning
- restore and cancel callbacks

## Current integration state

The component is not yet wired into `App.tsx`. This is intentional because `App.tsx` is still a large file and full-file updates have occasionally been blocked by connector safety checks.

## Next integration step

Add a new screen state:

```text
restore
```

Then wire:

```text
Seed screen -> RestoreScreen -> setWords(restoredWords) -> dashboard -> refresh API
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
