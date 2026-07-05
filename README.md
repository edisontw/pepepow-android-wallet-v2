# PEPEW Wallet

Experimental PEPEPOW wallet preview for rapid Google AI Studio / Vite testing and later native Android wallet design.

- App name: `PEPEW Wallet`
- Android package target: `net.pepepow.wallet`
- Public API: `https://light.pepepow.net/`
- Current phase: **Phase 4A restore preview scaffold**

## Security warning

This repository is still experimental.

- Use small test funds only.
- Current preview derivation is prototype logic, not reviewed production wallet-core.
- No production BIP39/BIP32/BIP44 compatibility is claimed yet.
- No encrypted storage, PIN, biometric lock, or auto-lock is implemented yet.
- Recovery words and signing logic must stay local.
- The public API should receive only addresses, UTXO/history requests, and signed raw transactions.

## Current working preview

The root `src/App.tsx` Vite preview currently supports:

- local test wallet open flow
- PEPEW P2PKH address derivation for preview testing
- balance lookup through PEPEW Light
- history lookup with wallet-oriented amount and direction fields
- UTXO lookup
- local transaction prepare/sign for small test funds
- review-before-broadcast card
- signed raw transaction broadcast through PEPEW Light
- post-broadcast optimistic update and delayed refresh
- manual refresh cooldown
- duplicate broadcast prevention

## Phase status

### Phase 1 — Mock wallet

Done.

### Phase 2 — Read-only API

Done.

### Phase 3 — Experimental send

Done for small-fund preview testing.

Key documents:

- `PHASE3_STATUS.md`
- `PHASE3_2_RELIABILITY.md`
- `PHASE3_3_STRUCTURE.md`

### Phase 4A — Restore preview

In progress.

Current scaffold:

- `PHASE4_RESTORE_PLAN.md`
- `PHASE4A_UI_STATUS.md`
- `src/restore/restoreTypes.ts`
- `src/restore/restoreValidation.ts`
- `src/restore/walletScanner.ts`
- `src/wallet/walletDeriver.ts`
- `src/screens/RestoreScreen.tsx`
- `src/screens/SeedStartScreen.tsx`

The restore screen components are ready, but final `App.tsx` wiring is still pending.

## Web preview development

```bash
npm install
npm run lint
npm run build
npm run dev
```

## GitHub Actions

A web preview workflow is available:

```text
.github/workflows/web-preview.yml
```

It runs:

```text
npm install
npm run lint
npm run build
```

## Suggested next work

1. Wire `SeedStartScreen` and `RestoreScreen` into `App.tsx`.
2. Continue splitting `App.tsx` into smaller screen components.
3. Keep restore prototype-only until wallet-core derivation is reviewed.
4. Add production restore design with receive/change chains, gap limit, history scan, and UTXO rebuild.
5. Add encrypted storage, PIN/biometric lock, and auto-lock in Phase 5.
