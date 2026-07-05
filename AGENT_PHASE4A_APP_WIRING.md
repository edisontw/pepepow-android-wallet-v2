# Agent Prompt — Phase 4A App Wiring

Use this prompt for Codex, Antigravity, AI Studio, or a local editor agent.

## Goal

Wire the already-added Phase 4A restore UI components into `src/App.tsx` without changing wallet behavior.

## Current files already added

```text
src/screens/SeedStartScreen.tsx
src/screens/RestoreScreen.tsx
src/screens/screenTitle.ts
src/types/wallet.ts
src/restore/restoreValidation.ts
src/wallet/walletDeriver.ts
```

## Required App changes

### 1. Import screen components

Add imports near the top of `src/App.tsx`:

```ts
import { SeedStartScreen } from "./screens/SeedStartScreen";
import { RestoreScreen } from "./screens/RestoreScreen";
import { screenTitle } from "./screens/screenTitle";
```

If `Screen` is still defined inline, either:

- update it to include `"restore"`, or
- import `Screen` from `src/types/wallet.ts`.

### 2. Add restore handler

Inside `App()`, add:

```ts
function restoreTestWallet(restoredWords: string[]) {
  setWords(restoredWords);
  setInputSeedMode(false);
  setCustomSeedInput("");
  setSendError("");
  setLocalTxs([]);
  setBroadcastResult(null);
  setLastRefreshAt(0);
  setWalletReady(true);
  setScreen("dashboard");
}
```

### 3. Replace seed screen block

Replace the existing `if (screen === "seed") { return (...) }` block with:

```tsx
if (screen === "seed") {
  return (
    <SeedStartScreen
      words={words}
      error={sendError}
      onCreate={() => {
        setWalletReady(true);
        setScreen("dashboard");
      }}
      onRestore={() => setScreen("restore")}
    />
  );
}
```

### 4. Add restore screen block before the main return

Add after the seed block:

```tsx
if (screen === "restore") {
  return (
    <div className="min-h-screen bg-[#eef7e9] pb-8 text-slate-900">
      <RestoreScreen
        onRestore={restoreTestWallet}
        onCancel={() => setScreen("seed")}
      />
    </div>
  );
}
```

### 5. Simplify Header title

Replace the inline title expression:

```tsx
screen === "receive" ? "Receive PEPEW" : screen === "send" ? "Send PEPEW" : screen === "history" ? "Transaction History" : "Wallet Configuration"
```

with:

```tsx
screenTitle(screen)
```

## Acceptance checks

Run:

```bash
npm run build
```

Then test in preview:

1. Start screen shows demo words.
2. `RESTORE TEST WALLET` opens restore screen.
3. Invalid word count shows validation error.
4. Valid 12 words preview a derived test address.
5. Restore opens dashboard.
6. Dashboard refreshes API for the restored local address.
7. Existing create/open, send, receive, history, settings flows still work.

## Safety boundary

This is still prototype restore only. Do not add production recovery claims. Do not persist recovery words. Do not send recovery words to the API.
