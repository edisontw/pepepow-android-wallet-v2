# Phase 3.2 Wallet Reliability Polish

Status: **In progress / partially implemented**

This document records the wallet reliability changes after live small-fund tests confirmed receive, balance, UTXO lookup, local signing, broadcast, and verbose backend history.

## Completed

- Normal history refresh no longer needs repeated transaction-detail API calls.
- History refresh now relies on backend wallet-friendly history rows.
- Send flow now shows a Review before broadcast card.
- Recipient and amount are locked after prepare until the user clears the prepared transaction.
- A submitted transaction cannot be submitted again from the same prepared payload.
- Post-broadcast optimistic update, delayed refresh, and manual refresh cooldown remain in place.

## New helper modules

Added reusable modules for the next cleanup pass:

```text
src/types/wallet.ts
src/config/appConstants.ts
src/utils/format.ts
src/api/walletApi.ts
```

`App.tsx` still owns the UI flow for now. These modules make later incremental migration safer and reduce future single-file update risk.

## Remaining cleanup

Move UI-only pieces into components:

```text
src/components/Header.tsx
src/components/TxCard.tsx
src/screens/SeedScreen.tsx
src/screens/DashboardScreen.tsx
src/screens/ReceiveScreen.tsx
src/screens/SendScreen.tsx
src/screens/HistoryScreen.tsx
src/screens/SettingsScreen.tsx
```

Move API parsing fully into:

```text
src/api/walletApi.ts
```

## Acceptance criteria

- History refresh no longer makes repeated transaction-detail calls during normal refresh. ✅
- Dashboard history still shows correct sent/received amounts. ✅
- Send page shows review details before broadcast. ✅
- Prepare button remains hidden after prepare until user clears prepared transaction. ✅
- Broadcast button cannot be repeatedly pressed for the same prepared transaction. ✅
- Balance remains correct after broadcast and after API refresh. ✅

## Notes

This remains an experimental test wallet. Do not move to production security work until wallet-core derivation/signing is independently reviewed.
