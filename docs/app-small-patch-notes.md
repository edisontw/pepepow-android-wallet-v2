# App Small Patch Notes

`src/App.tsx` is still a large file, so apply only small targeted edits.

## 1. Create Wallet should not open the old fixed wallet

Add import:

```ts
import { createRecoveryWords } from "./wallet/recoveryWords";
```

Replace:

```ts
const WORDS = ["swamp", "pepe", "key", "power", "wallet", "frog", "meme", "blockchain", "pond", "green", "crypto", "speed"];
```

with no constant, and replace:

```ts
const [words, setWords] = useState(WORDS);
```

with:

```ts
const [words, setWords] = useState<string[]>(() => createRecoveryWords());
```

In the `SeedStartScreen` create callback, call:

```ts
setWords(createRecoveryWords());
setWalletReady(true);
setScreen("dashboard");
```

## 2. WIF reveal should use SensitiveRevealCard

Add import:

```ts
import { SensitiveRevealCard } from "./screens/SensitiveRevealCard";
```

Remove direct `showWif` state and the direct WIF reveal section.

Replace it with:

```tsx
<SensitiveRevealCard
  title="Private Key WIF"
  description="Anyone with this private key can spend funds from this wallet. Reveal it only in a private environment."
  value={localWallet.wif}
  revealLabel="REVEAL PRIVATE KEY"
/>
```

## 3. Self-transfer history should not show as a negative send

Add import:

```ts
import { classifyTransactionForAddress } from "./wallet/transactionClassifier";
```

Add `isSelfTransfer?: boolean` to `Tx`.

In `parseApiHistory()`, after `delta` is calculated:

```ts
const kind = classifyTransactionForAddress(item, address, delta);
const isSelfTransfer = kind === "self";
const isSend = kind === "sent";
```

Include `isSelfTransfer` in the returned `Tx`.

In `TxCard()`:

```ts
const title = tx.isUnknownAmount ? "Wallet Transaction" : tx.isSelfTransfer ? "Self Transfer" : tx.isSend ? "Sent PEPEW" : "Received PEPEW";
const sign = tx.isUnknownAmount || tx.isSelfTransfer ? "" : tx.isSend ? "-" : "+";
```

## Build check

```bash
npm run lint
npm run build
```
