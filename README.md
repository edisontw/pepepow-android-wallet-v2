# PEPEW Wallet

Experimental PEPEPOW wallet preview for rapid Google AI Studio / Vite testing and later native Android wallet design.

- App name: `PEPEW Wallet`
- Android package target: `net.pepepow.wallet`
- Public API: `https://light.pepepow.net/`
- Current phase: **Phase 5 security scaffold**

## Security warning

This repository is still experimental.

- Use small test funds only.
- Current preview derivation is prototype logic, not reviewed production wallet-core.
- No production BIP39/BIP32/BIP44 compatibility is claimed yet.
- Phase 5 is starting the security scaffold, but encrypted storage, PIN, biometric lock, and auto-lock are not audited production security yet.
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

### Phase 4 — Restore preview

Completed as a preview milestone.

Key documents and scaffold:

- `PHASE4_RESTORE_PLAN.md`
- `PHASE4A_UI_STATUS.md`
- `src/restore/restoreTypes.ts`
- `src/restore/restoreValidation.ts`
- `src/restore/walletScanner.ts`
- `src/wallet/walletDeriver.ts`
- `src/screens/RestoreScreen.tsx`
- `src/screens/SeedStartScreen.tsx`

Keep restore prototype-only until wallet-core derivation is reviewed.

### Phase 5 — Security scaffold

Started.

Key documents:

- `PHASE5_SECURITY_PLAN.md`
- `AGENT_PHASE5_SECURITY_SCAFFOLD.md`

Phase 5 priority:

1. Add security domain types and lock-state reducer.
2. Add placeholder security repository/store.
3. Add PIN/biometric UI scaffold without claiming production security.
4. Add auto-lock timer wiring.
5. Gate seed reveal and wallet reset behind explicit confirmation.
6. Add tests for lock state transitions.
7. Document remaining native Android Keystore work.

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

1. Run the Phase 5 security scaffold agent prompt.
2. Keep all mnemonic, seed, private-key, and signing material local.
3. Do not claim production wallet security until native encrypted storage and wallet-core logic are reviewed.
4. Continue splitting `App.tsx` into smaller screen components.
5. Add production restore design with receive/change chains, gap limit, history scan, and UTXO rebuild.
