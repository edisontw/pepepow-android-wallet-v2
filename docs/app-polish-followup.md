# App Polish Follow-up

Studio test status:

- Settings recovery words reveal works.
- Wipe and reset works.
- Receive and send work.
- Backup screen shows 12 words.

Remaining polish:

1. Dashboard/start logo
   - Prefer built-in CSS/text mark instead of external image path.
   - `SeedStartScreen` has already been changed to use a built-in `PW` mark.
   - Dashboard should also show the same `PW` mark beside `PEPEW WALLET`.

2. Self transfer display
   - Self-transfer history should display `0.00`, not the sent amount.
   - Local optimistic self-transfer should store amount `0` or TxCard should display `0` when `isSelfTransfer` is true.

3. Backup screen copyable phrase
   - Add one compact row showing the full 12-word recovery phrase.
   - Tapping the row should copy the phrase.
   - Keep the numbered word grid.
   - Compress the warning and word cards slightly to fit better on mobile.

Suggested exact App changes:

```ts
const displayAmount = tx.isSelfTransfer ? 0 : tx.amount;
```

Then render:

```tsx
{tx.isUnknownAmount ? "—" : `${sign}${formatAmount(displayAmount, 2)}`}
```

For local optimistic self-transfer:

```ts
amount: isSelfTransfer ? 0 : amount,
```

For dashboard logo, replace the plain `PEPEW WALLET` text with a small inline `PW` mark.

For backup copy row, use existing `handleCopy(words.join(" "), "recovery words")`.
