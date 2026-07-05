import { initialSecuritySession, isWalletUsable, securityReducer } from "./securityReducer";

function assert(condition: boolean, message: string) {
  if (!condition) throw new Error(message);
}

export function runSecurityReducerSelfTest() {
  const noWallet = securityReducer(initialSecuritySession, { type: "BOOT", hasWallet: false });
  assert(noWallet.state === "NO_WALLET", "BOOT without wallet should enter NO_WALLET");
  assert(!isWalletUsable(noWallet), "NO_WALLET should not be usable");

  const locked = securityReducer(initialSecuritySession, { type: "BOOT", hasWallet: true });
  assert(locked.state === "WALLET_LOCKED", "BOOT with wallet should enter WALLET_LOCKED");
  assert(!isWalletUsable(locked), "WALLET_LOCKED should not be usable");

  const unlocked = securityReducer(locked, { type: "UNLOCK" });
  assert(unlocked.state === "WALLET_UNLOCKED", "UNLOCK should enter WALLET_UNLOCKED");
  assert(isWalletUsable(unlocked), "WALLET_UNLOCKED should be usable");

  const revealPending = securityReducer(unlocked, { type: "REQUEST_SEED_REVEAL" });
  assert(revealPending.state === "REVEAL_SEED_PENDING", "REQUEST_SEED_REVEAL should enter REVEAL_SEED_PENDING");
  assert(isWalletUsable(revealPending), "REVEAL_SEED_PENDING should keep wallet usable");

  const resetPending = securityReducer(unlocked, { type: "REQUEST_RESET" });
  assert(resetPending.state === "RESET_PENDING", "REQUEST_RESET should enter RESET_PENDING");
  assert(isWalletUsable(resetPending), "RESET_PENDING should keep wallet shell usable for confirmation");

  const resetDone = securityReducer(resetPending, { type: "CONFIRM_RESET" });
  assert(resetDone.state === "NO_WALLET", "CONFIRM_RESET should enter NO_WALLET");

  return true;
}
