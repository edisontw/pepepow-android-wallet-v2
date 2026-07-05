# PEPEW Wallet Documentation

Documentation for PEPEW Wallet is collected in this folder.

## Core documents

- [`pepepow-parameters.md`](./pepepow-parameters.md) — PEPEPOW network, address, WIF, and HD wallet derivation parameters.
- [`android-icon-setup.md`](./android-icon-setup.md) — Android launcher icon setup notes.
- [`release-polish-status.md`](./release-polish-status.md) — UI polish status and manual verification checklist.
- [`development-history.md`](./development-history.md) — short record of completed development milestones.
- [`app-small-patch-notes.md`](./app-small-patch-notes.md) — targeted App patch notes for Codex/Studio handoff.
- [`github-connector-notes.md`](./github-connector-notes.md) — observed GitHub connector write limits and workaround flow.

## Current wallet rules

Default derivation rule:

```text
BIP39 -> BIP32 -> m/44'/5'/0'/0/0 -> pubkeyToP2PKH
```

PEPEPOW constants:

```text
addressHeader = 55 = 0x37
dumpedPrivateKeyHeader = 204 = 0xCC
p2shHeader = 16 = 0x10
coinType = 5
packetMagic = 0xbf0c6bbd
port = 8833
```

## Documentation policy

Keep current usage and parameter documents in `docs/`. Temporary planning notes should be summarized into `development-history.md` when they are no longer needed.
