# Phase 3 Experimental Attempt

Branch: `phase3-experimental-full`

## Goal

Try to prepare a full experimental Phase 3 wallet path for Google AI Studio testing.

Requested target:

- local wallet generation/import,
- PEPEW address display,
- balance/history lookup via PEPEW Light API,
- UTXO lookup,
- local transaction preparation,
- signed raw transaction submission.

## What was committed

This branch currently contains only safe setup changes:

1. Experimental dependency preparation in `package.json`:
   - `@noble/curves`
   - `@noble/hashes`
   - `bs58`
2. PEPEPOW network parameter constants in `src/wallet/pepepowParams.ts`:
   - P2PKH version: `55`
   - P2SH version: `16`
   - WIF version: `204`
   - coin type: `5`
   - API base: `https://light.pepepow.net`

## What did not commit

Attempts to commit a complete wallet helper and full preview implementation were blocked by the tool safety layer.

Blocked areas included:

- wallet secret generation/import helper,
- WIF encode/decode helper,
- transaction construction helper,
- local approval/signing helper,
- signed raw transaction submission UI.

## Practical next path

Use this branch as a starting point in Google AI Studio or a local clone, then add the full experimental implementation there and test before merging.

Recommended manual workflow:

1. Checkout this branch.
2. Run install so the new crypto/base58 libraries are available.
3. Add wallet helper code locally or inside Google AI Studio.
4. First test address generation and Receive.
5. Send a tiny amount to the generated address.
6. Verify balance/history.
7. Only then test UTXO, local transaction preparation, and signed raw submission.

## Safety note

Use tiny test amounts only. This branch is not production wallet code.
