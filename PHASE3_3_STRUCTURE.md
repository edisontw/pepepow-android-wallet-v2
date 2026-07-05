# Phase 3.3 Structure Cleanup

Status: **Scaffold added**

This phase starts reducing the risk of maintaining the wallet preview as one large `src/App.tsx` file.

## Added modules

```text
src/types/wallet.ts
src/config/appConstants.ts
src/utils/format.ts
src/api/walletApi.ts
src/components/Header.tsx
src/components/TxCard.tsx
```

## Purpose

The new modules separate stable concerns:

- shared wallet UI types
- app/API constants
- formatting helpers
- wallet API parsing and snapshot fetching
- reusable header component
- reusable transaction card component

## Current state

`App.tsx` still owns the full UI flow. The modules are present so the next pass can safely migrate imports with smaller changes.

## Next migration steps

1. Replace inline type definitions in `App.tsx` with imports from `src/types/wallet.ts`.
2. Replace inline formatting helpers with imports from `src/utils/format.ts`.
3. Replace inline wallet API parsing with `fetchWalletSnapshot()` and `parseUtxos()` from `src/api/walletApi.ts`.
4. Replace inline Header and TxCard with `src/components/Header.tsx` and `src/components/TxCard.tsx`.
5. Then split full screens into `src/screens/*`.

## Acceptance criteria

- Studio import still builds.
- Wallet behavior remains unchanged.
- `App.tsx` becomes smaller.
- Future changes can target small files instead of a single large file.
