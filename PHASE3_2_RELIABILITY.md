# Phase 3.2 Wallet Reliability Polish

Status: **Planned / next implementation pass**

This document records the next wallet reliability changes after live small-fund tests confirmed receive, balance, UTXO lookup, local signing, broadcast, and verbose backend history.

## Goals

Keep the wallet experimental, but make the current send/history flow clearer and less server-heavy.

## Backend dependency

The backend now returns wallet-friendly history rows by default:

```text
GET /api/wallet/history/{address}?limit=50&offset=0
```

Rows include:

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

Therefore the frontend should not bulk-call `/api/wallet/tx/{txid}` during normal history refresh.

## Frontend changes to apply

### 1. Remove normal tx-detail fallback calls

Current frontend still has a fallback path that can call:

```text
/api/wallet/tx/{txid}
```

for each history row. That was needed before verbose backend history existed. It should now be removed from normal refresh flow.

Keep fallback only behind an explicit condition:

```text
if no history row has address_delta_pepew/address_delta_atoms
```

or remove it entirely for the AI Studio prototype.

### 2. Explicit verbose history URL

Use:

```text
/api/wallet/history/{address}?limit=50&offset=0&detail_limit=10
```

`verbose=true` is now backend default, but keeping `detail_limit=10` documents intent and limits backend work.

For forced refresh:

```text
/api/wallet/history/{address}?limit=50&offset=0&detail_limit=10&fresh=1&t={timestamp}
```

### 3. Review block before broadcast

After `PREPARE & SIGN TRANSACTION`, show a clear review card:

```text
Review before broadcast
From: active address
To: recipient
Amount: X PEPEW
Fee: 0.001 PEPEW
Total spent: X + fee
Input total: current UTXO total
Estimated change: input total - amount - fee
```

Keep recipient and amount locked while signed raw tx exists.

### 4. Duplicate broadcast prevention

Keep current disabled state for broadcast while broadcasting, and also disable broadcast after success until the user edits details or starts a new transaction.

### 5. Post-broadcast state

Current behavior is good:

- optimistic balance update
- local pending tx
- delayed auto refresh
- manual refresh with cooldown

Keep refresh intervals conservative:

```text
12 seconds
45 seconds
```

Manual refresh cooldown:

```text
15 seconds
```

## Acceptance criteria

- History refresh no longer makes N tx-detail calls when backend verbose fields are present.
- Dashboard history still shows correct sent/received amounts.
- Send page shows review details before broadcast.
- Prepare button remains hidden after prepare until user clears signed tx.
- Broadcast button cannot be repeatedly pressed for the same signed tx.
- Balance remains correct after broadcast and after API refresh.

## Notes

This remains an experimental test wallet. Do not move to production security work until wallet-core derivation/signing is independently reviewed.
