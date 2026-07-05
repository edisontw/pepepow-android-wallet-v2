# Phase 3 Status

Status: **AI Studio Phase 3 experimental wallet preview merged to `main`**

## Scope

This phase is now an experimental browser-preview wallet for small test amounts. It is intended for rapid AI Studio / Vite testing, not production use.

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

## Safety boundary

This is **not** production wallet code.

Known limitations:

- The prototype uses simplified `sha256(seed phrase)` derivation, not reviewed BIP39/BIP32/BIP44 wallet-core logic.
- WIF reveal is enabled for testing only.
- Fee/change logic is minimal.
- UTXO parsing is best-effort against current PEPEW Light API response variants.
- No encrypted persistent wallet storage is implemented.
- No PIN, biometric lock, or auto-lock is implemented.
- Use small test funds only.

## Current test status

Observed before merge:

- Receive: PASS
- Balance: PASS
- History: PASS
- UTXO/send: under test
- Broadcast: under test

The send path now includes broader UTXO parser support for fields such as `value_atoms`, `amount_atoms`, `atoms`, `satoshis`, `value`, `amount`, `txid`, `tx_hash`, `vout`, and `tx_pos`.

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
