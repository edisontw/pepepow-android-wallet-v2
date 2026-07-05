# Phase 3 Status

Status: **safe mock send scaffold added to Google AI Studio / Vite preview**

## Scope

This phase adds a visible Send flow for testing UX only.

Implemented in the preview:

- Recipient address validation.
- Amount validation.
- Mock fee display.
- Local mock transaction preparation.
- Local mock confirmation.
- Pending local transaction inserted into dashboard/history state.

## Safety boundary

This is not a production send implementation.

Not implemented:

- Real seed handling.
- Real key derivation.
- Real address derivation.
- Real UTXO selection.
- Real transaction construction.
- Real approval/signing.
- Real network submission.

No backend submit call is made by the Phase 3 preview Send flow.

## Test path

In Google AI Studio preview:

1. Generate seed phrase.
2. Check all confirmation boxes.
3. Enter wallet.
4. Open Send.
5. Enter a P-prefixed address and positive amount.
6. Tap `PREPARE MOCK SEND`.
7. Review the local mock summary.
8. Tap `CONFIRM LOCAL MOCK`.
9. Confirm Dashboard shows a pending local mock send transaction.

## Next real implementation notes

Before production send is added, the app needs independently reviewed wallet-core logic:

- deterministic address derivation,
- encrypted local storage,
- fee/change calculation,
- UTXO selection,
- transaction construction,
- local approval/signing,
- signed raw transaction submission only.
