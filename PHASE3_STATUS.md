# Phase 3 Status

Status: **Phase 3 experimental wallet works with live PEPEW Light API test funds**

## Scope

This phase is an experimental browser-preview wallet for small test amounts. It is intended for rapid AI Studio / Vite testing, not production use.

Implemented in the preview:

- 12-word seed import / demo seed flow.
- Deterministic local private-key derivation for testing.
- PEPEW P2PKH address derivation with version byte `55`.
- WIF display with version byte `204`.
- Balance/history lookup via `https://light.pepepow.net`.
- Live UTXO lookup via `/api/wallet/utxo/{address}`.
- Local P2PKH transaction construction and secp256k1 signing.
- Signed transaction preview.
- Live network submit through the PEPEW Light wallet endpoint.
- Demo read-only address toggle for API testing.
- API refresh cooldown and manual refresh button.
- Post-broadcast optimistic balance update and delayed API refresh.

## Safety boundary

This is **not** production wallet code.

Known limitations:

- The prototype uses simplified `sha256(seed phrase)` derivation, not reviewed BIP39/BIP32/BIP44 wallet-core logic.
- WIF reveal is enabled for testing only.
- Fee/change logic is minimal.
- UTXO selection is simple first-fit.
- No encrypted persistent wallet storage is implemented.
- No PIN, biometric lock, or auto-lock is implemented.
- Use small test funds only.

## Current test status

Observed with live small funds on `PPcRmmbFbbSr4kEe2xNKarckfh2WFQuejW`:

- Receive: PASS
- Balance: PASS
- History amount/direction: PASS after backend verbose history update
- UTXO lookup: PASS
- Prepare/sign: PASS
- Broadcast: PASS
- Post-broadcast refresh: PASS

Current known good live API behavior:

- `/api/wallet/history/{address}` now returns wallet-friendly `direction`, `amount_pepew`, `address_delta_pepew`, `timestamp`, and `confirmations` by default.
- `/api/wallet/utxo/{address}?fresh=1` returns the current spendable output and total atoms.

## Backend dependency

The wallet UI now relies on PEPEW Light backend history rows containing address-level deltas. The backend repo is:

```text
edisontw/pepepow-electrumx-service
```

Relevant backend behavior:

```text
GET /api/wallet/history/{address}?limit=50&offset=0
```

The endpoint defaults to verbose wallet history and returns:

```text
direction
amount_atoms
amount_pepew
address_delta_atoms
address_delta_pepew
received_atoms
spent_atoms
timestamp
confirmations
```

## Next implementation notes

Before production release, replace the experimental wallet core with independently reviewed logic:

- BIP39 mnemonic generation/import.
- BIP32/BIP44 or PEPEW-specific derivation path.
- Address gap scan and restore flow.
- Audited UTXO selection.
- Fee/change policy.
- Transaction signing test vectors.
- Encrypted local storage.
- PIN / biometric / auto-lock.
- External wallet-core review.

## Suggested next phase

Phase 3.2 should focus on wallet reliability polish, not new cryptography:

- Reduce frontend transaction-detail fallback calls once backend verbose history is confirmed stable.
- Add a clearer Review screen before broadcast.
- Add explicit fee/change display.
- Add duplicate broadcast prevention and stronger post-broadcast state handling.
- Add basic test vectors for address/WIF/transaction serialization if a test runner is added.
