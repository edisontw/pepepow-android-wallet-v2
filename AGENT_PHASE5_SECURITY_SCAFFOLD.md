# Agent Prompt — Phase 5 Security Scaffold

Use this prompt with Codex / Antigravity / AI Studio after Phase 4 restore work is complete.

## Goal

Start Phase 5 for PEPEW Wallet by adding a security scaffold around wallet access, seed reveal, wallet reset, and future encrypted storage.

This is **not** a production security implementation yet. It should create safe structure without making false claims.

## Repository

```text
edisontw/pepepow-android-wallet-v2
```

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

## Allowed implementation

Add lightweight security scaffolding:

```text
src/security/securityTypes.ts
src/security/securityReducer.ts
src/security/securityStore.ts
src/security/useAutoLock.ts
src/screens/LockScreen.tsx
src/screens/SecuritySettingsScreen.tsx
```

If the current file structure differs, adapt naturally, but keep security code isolated under `src/security/` where practical.

## Required security states

Implement or document these states:

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

Add scaffold behavior for:

1. Lock wallet
2. Unlock wallet with placeholder PIN flow
3. Optional placeholder biometric button
4. Auto-lock after inactivity
5. Require confirmation before seed reveal
6. Require typed confirmation before wallet reset
7. Display clear non-custodial warning

## Do not implement yet

Do not add production-level cryptography in this pass:

```text
real new mnemonic generation
new private-key derivation logic
new signing logic
new UTXO selection logic
backend custody
cloud backup
remote seed recovery
```

Do not store secrets in plain localStorage as a production path.

## Suggested placeholder interfaces

```ts
export type SecurityState =
  | 'UNINITIALIZED'
  | 'NO_WALLET'
  | 'WALLET_LOCKED'
  | 'WALLET_UNLOCKED'
  | 'REVEAL_SEED_PENDING'
  | 'SESSION_EXPIRED'
  | 'RESET_PENDING'

export interface SecurityStore {
  hasWallet(): Promise<boolean>
  lock(): Promise<void>
  unlockWithPin(pin: string): Promise<boolean>
  unlockWithBiometric(): Promise<boolean>
  markSessionActive(): void
  clearWallet(): Promise<void>
}
```

## Suggested UI copy

```text
Your recovery words control your PEPEW. Anyone with these words can spend your funds.
PEPEW Light API never needs your recovery words or private keys.
Only signed raw transactions should be broadcast to the API.
```

## Acceptance criteria

- App builds.
- Existing Phase 4 restore/send preview behavior still works.
- A lock state exists and can be entered manually.
- Seed reveal path has warning + confirmation scaffold.
- Reset wallet path has destructive confirmation scaffold.
- README says Phase 5 is a security scaffold, not audited production security.
- No mnemonic/seed/private key is sent to the API.
- No false claim that storage is production encrypted yet.

## Testing

Run:

```bash
npm install
npm run lint
npm run build
```

If tests exist, run them too.

## Commit message

```text
Add Phase 5 security scaffold
```
