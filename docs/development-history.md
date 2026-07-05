# Development History

This file keeps a short record of the early development phases. Detailed phase scaffolding files were removed after the UI was polished and the documentation was consolidated.

## Milestones

| Milestone | Summary |
| --- | --- |
| Mock wallet | Created initial PEPEW Wallet UI flow with local mock wallet data. |
| Read-only API | Connected balance, history, status, and UTXO lookups to PEPEW Light API. |
| Send flow | Added local transaction preparation, review-before-broadcast, broadcast, and local optimistic history. |
| Restore flow | Added recovery phrase restore screen and default derivation path design. |
| Security scaffold | Added non-custodial boundary, sensitive reveal warnings, reset confirmation concepts, and local-only key handling rules. |
| Release polish | Removed public-facing phase/test wording, removed demo address toggle, added recent recipient history, and simplified the UI. |

## Current documentation

Use these files for current work:

- [`README.md`](../README.md)
- [`docs/pepepow-parameters.md`](./pepepow-parameters.md)
- [`docs/android-icon-setup.md`](./android-icon-setup.md)
- [`docs/release-polish-status.md`](./release-polish-status.md)

## Current wallet rule

```text
BIP39 -> BIP32 -> m/44'/5'/0'/0/0 -> pubkeyToP2PKH
```

## Current non-custodial boundary

The app keeps mnemonic, seed, private key, WIF, and signing logic local. The API should receive only address queries, UTXO queries, and signed raw transactions.
