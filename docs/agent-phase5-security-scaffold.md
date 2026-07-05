# Agent Prompt — Phase 5 Security Scaffold

Use this prompt with Codex / Antigravity / AI Studio for security-scaffold work.

## Goal

Add a security scaffold around wallet access, seed reveal, wallet reset, and future encrypted storage.

This is not a production security implementation yet. It should create safe structure without making false claims.

## Core rules

Keep the wallet non-custodial.

Never send any of these to `https://light.pepepow.net/` or any backend:

```text
mnemonic
seed
private key
WIF
unsigned sensitive wallet material
```

The API may only receive:

```text
address
balance/history requests
UTXO requests
signed raw transactions for broadcast
```

## Required security states

```text
UNINITIALIZED
NO_WALLET
WALLET_LOCKED
WALLET_UNLOCKED
REVEAL_SEED_PENDING
SESSION_EXPIRED
RESET_PENDING
```

## Required UX behavior

- Lock wallet
- Unlock wallet with PIN / biometric flow
- Auto-lock after inactivity
- Require confirmation before seed reveal
- Require typed confirmation before wallet reset
- Display clear non-custodial warning

## Do not implement without review

```text
new production mnemonic generation
new private-key derivation logic
new signing logic
new UTXO selection logic
backend custody
cloud backup
remote seed recovery
```

Do not store secrets in plain localStorage as a production path.

## Acceptance criteria

- App builds.
- Seed reveal path has warning + confirmation.
- Reset wallet path has destructive confirmation.
- No mnemonic/seed/private key is sent to the API.
- No false claim that storage is production encrypted yet.

## Testing

```bash
npm install
npm run lint
npm run build
```
