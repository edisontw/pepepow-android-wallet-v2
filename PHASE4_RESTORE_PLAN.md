# Phase 4 Restore Plan

Status: **Ready to start after Phase 3 live-send validation**

Phase 3 is considered usable for the experimental browser preview:

- receive works
- balance works
- history amount and direction work
- UTXO lookup works
- local transaction prepare/sign works for small test funds
- broadcast works
- post-broadcast refresh works
- repeated broadcast prevention is in place

Phase 4 should focus on restore and address discovery. Do not treat the current prototype seed logic as production wallet-core code.

## Goal

Allow a user to restore a wallet from a mnemonic-like phrase and discover wallet activity without sending any recovery material to the backend.

```text
Mnemonic / seed stays local.
Address derivation stays local.
API only receives derived public addresses.
```

## Important boundary

The current Phase 3 preview uses simplified deterministic test derivation. Phase 4 should be split into two tracks:

1. **Prototype restore UI** using the existing test derivation, for Studio and flow validation.
2. **Production restore design** waiting for reviewed wallet-core derivation, address path, and signing test vectors.

Do not claim production BIP39/BIP32/BIP44 compatibility until independently reviewed and tested.

## Phase 4A — Prototype restore UI

Implement first because it is low-risk and useful for testing.

### Screens / UX

Add or improve:

- Restore wallet entry point on the seed screen.
- 12-word input validation.
- Local derived address preview.
- Warning that this is experimental test-wallet derivation only.
- Restore action that opens the dashboard and refreshes API state.

### Acceptance criteria

- User can paste 12 words.
- App validates word count and empty words.
- App derives the same test address for the same phrase.
- App refreshes balance/history/UTXO after restore.
- No mnemonic or private key is sent to the API.

## Phase 4B — Address discovery / scan design

Production restore will need address discovery, not just one address.

### Required concepts

```text
external receive chain
internal change chain
gap limit
used address detection
history scan
UTXO rebuild
```

### Prototype scan approach

Before production wallet-core is ready, implement the design as an interface, not final cryptography:

```text
WalletDeriver
  deriveReceiveAddress(index)
  deriveChangeAddress(index)

WalletScanner
  scanReceiveChain(gapLimit)
  scanChangeChain(gapLimit)
  collectHistory()
  collectUtxos()
```

The first implementation can still return one test address, but the UI and repository boundaries should be ready for multi-address restore.

## API needs

Current API is enough for first prototype:

```text
GET /api/wallet/address/{address}
GET /api/wallet/history/{address}
GET /api/wallet/utxo/{address}
```

For better multi-address scan later, consider adding backend batch endpoints:

```text
POST /api/wallet/batch/address
POST /api/wallet/batch/history
POST /api/wallet/batch/utxo
```

Batch endpoints must accept only public addresses, never mnemonic, seed, private key, WIF, xprv, or signing material.

## Suggested file structure

```text
src/restore/
  restoreTypes.ts
  restoreValidation.ts
  walletScanner.ts

src/wallet/
  walletDeriver.ts

src/screens/
  RestoreScreen.tsx
  RestoreScanScreen.tsx
```

## First implementation tasks

1. Add a clear Restore mode to the seed screen.
2. Add validation helpers for phrase length and normalized words.
3. Add a local restore flow that reuses current test derivation.
4. Add restore warnings and safety copy.
5. Add interfaces for future multi-address scan.
6. Add documentation that current restore is prototype-only.

## Security notes

- Never log mnemonic or private key.
- Never send mnemonic or private key over network.
- Do not store seed persistently until encrypted storage exists.
- Do not add production claims before wallet-core review.

## When Phase 4 is complete

Phase 4 can be considered complete when:

- restore UI works for the experimental test phrase flow
- architecture is ready for multi-address scan
- production wallet-core requirements are documented
- no secret material leaves local runtime
