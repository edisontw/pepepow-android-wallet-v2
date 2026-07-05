# Release Polish Status

This pass makes the PEPEW Wallet UI more production-facing.

## Completed

- Removed public-facing Phase 5 wording from the onboarding screen.
- Removed public-facing test-wallet and small-fund warnings from the onboarding screen.
- Replaced onboarding copy with non-custodial wallet messaging.
- Removed the demo address constant from `src/App.tsx`.
- Removed the `Use demo address for testing` setting from the Settings screen.
- Cleared the default recipient address so no test address is prefilled.
- Removed floating security shell controls from the main UI.
- Reworded Lock screen text to production-facing copy.
- Reworded Reset confirmation text to production-facing copy.
- Added recent recipient address storage in `src/wallet/recentRecipients.ts`.
- Added `RecentRecipientsCard` for quick recipient selection.
- Connected recent recipients to the Send flow:
  - successful broadcast stores the recipient address
  - Send screen shows recent recipients
  - selecting a recent recipient fills the recipient field
  - user can clear recipient history

## Still intentionally preserved

- Non-custodial security notes.
- Sensitive private key reveal guard paths.
- Local signing / API boundary messaging.
- API cooldown and UTXO status indicators.

## Manual verification checklist

Run:

```bash
npm install
npm run lint
npm run build
```

Then verify:

- Onboarding no longer says Phase 5, test wallet, preview, or small test funds.
- Settings no longer has `Use demo address for testing`.
- Send screen starts with an empty recipient field.
- After a successful broadcast, the recipient appears in Recent Recipients.
- Clicking a Recent Recipient fills the recipient field.
- Clear button removes the recent recipient list.
- Lock screen no longer shows preview/scaffold wording.
- Android launcher icon still uses `@drawable/pepew_logo`.

## Notes

Documentation files may still mention phase history for development traceability. Public-facing UI should avoid phase/test wording.
