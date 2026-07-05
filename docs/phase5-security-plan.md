# Phase 5 Security Plan

Phase 5 starts the security hardening track for PEPEW Wallet.

This phase must keep the wallet **non-custodial**:

```text
API provides blockchain data.
App keeps seed/private key locally.
App builds/signs transactions locally.
API only handles balance/history/UTXO + broadcast.
```

## Scope

Phase 5 is a design and scaffold phase first. Do not replace the experimental wallet-core logic with production cryptography in this pass.

Allowed work:

- security threat model
- encrypted local storage interface
- PIN lock UI scaffold
- biometric unlock UI scaffold
- auto-lock state machine scaffold
- seed reveal warning and confirmation UX
- clipboard/screenshot warnings
- wallet reset confirmation UX
- tests for lock/unlock state transitions
- documentation of production requirements

Not allowed in this phase pass:

- sending mnemonic, seed, or private key to any API
- backend custody
- remote recovery phrase backup
- real production mnemonic generation unless reviewed separately
- real production private-key derivation unless reviewed separately
- unreviewed signing changes
- silent export of seed/private key

## Security states

Use explicit app-level lock states:

```text
UNINITIALIZED
NO_WALLET
WALLET_LOCKED
WALLET_UNLOCKED
REVEAL_SEED_PENDING
SESSION_EXPIRED
RESET_PENDING
```

| State | Meaning | Allowed actions |
| --- | --- | --- |
| `UNINITIALIZED` | App boot/loading | Load local wallet metadata only |
| `NO_WALLET` | No local wallet exists | Create / restore wallet |
| `WALLET_LOCKED` | Wallet exists but session locked | PIN/biometric unlock, reset flow |
| `WALLET_UNLOCKED` | Active wallet session | Balance/history/send/receive |
| `REVEAL_SEED_PENDING` | User requested seed display | Require re-auth + warning confirmation |
| `SESSION_EXPIRED` | Auto-lock timeout reached | Return to lock screen |
| `RESET_PENDING` | User requested destructive reset | Require typed confirmation |

## Storage boundary

Production design target:

```text
EncryptedStorage stores encrypted wallet material locally.
Android Keystore protects encryption keys.
PIN/biometric unlock only gates local decryption.
```

Do not persist real mnemonic/private key in plain localStorage for production.

## Auto-lock rules

Initial target behavior:

- lock immediately when user taps Lock
- lock after configurable inactivity timeout
- lock on tab/app visibility hidden after a grace period
- require re-auth before revealing seed words
- require re-auth before signing or broadcasting transaction in production builds

Suggested default timeout:

```text
5 minutes
```

## UI warnings

Required warning surfaces:

- Create wallet / restore wallet warning
- Backup phrase warning
- Reveal seed warning
- Send confirmation warning
- Reset wallet destructive warning
- API privacy note: API receives address/UTXO/history/broadcast requests only

Suggested copy:

```text
Your recovery words control your PEPEW. Anyone with these words can spend your funds.
PEPEW Light API never needs your recovery words or private keys.
Only signed raw transactions should be broadcast to the API.
```

## Native Android follow-up

When the project moves from web preview back to native Android, implement:

- `EncryptedSharedPreferences` or Jetpack Security Crypto equivalent
- Android Keystore key generation
- BiometricPrompt integration
- PIN retry limits and lockout policy
- root/debug warning policy if needed
- secure screen flag for seed/private-key display screens
- no sensitive data in logs, crash reports, analytics, or screenshots

## Acceptance checklist

- [ ] App has explicit lock/unlock state model.
- [ ] Seed reveal requires warning and re-auth scaffold.
- [ ] Wallet reset requires destructive confirmation.
- [ ] Auto-lock rules are documented and scaffolded.
- [ ] API boundary remains non-custodial.
- [ ] No mnemonic/seed/private key is sent to API.
- [ ] No production security claim is made before review.
