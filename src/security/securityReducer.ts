import type { SecurityAction, SecuritySession } from "./securityTypes";

export const initialSecuritySession: SecuritySession = {
  state: "UNINITIALIZED",
  lastActiveAt: Date.now(),
};

export function securityReducer(session: SecuritySession, action: SecurityAction): SecuritySession {
  const now = Date.now();

  switch (action.type) {
    case "BOOT":
      return {
        state: action.hasWallet ? "WALLET_LOCKED" : "NO_WALLET",
        lastActiveAt: now,
      };

    case "CREATE_OR_RESTORE_WALLET":
      return {
        state: "WALLET_UNLOCKED",
        lastActiveAt: now,
        message: "Wallet session unlocked for this preview session.",
      };

    case "LOCK":
      return {
        ...session,
        state: "WALLET_LOCKED",
        lastActiveAt: now,
        message: "Wallet locked.",
      };

    case "UNLOCK":
      return {
        ...session,
        state: "WALLET_UNLOCKED",
        lastActiveAt: now,
        unlockMethod: "PIN_PLACEHOLDER",
        message: "Unlocked with placeholder PIN flow.",
      };

    case "REQUEST_SEED_REVEAL":
      return {
        ...session,
        state: "REVEAL_SEED_PENDING",
        lastActiveAt: now,
        message: "Confirm before revealing sensitive wallet material.",
      };

    case "CANCEL_SEED_REVEAL":
      return {
        ...session,
        state: "WALLET_UNLOCKED",
        lastActiveAt: now,
      };

    case "SESSION_EXPIRED":
      return {
        ...session,
        state: "SESSION_EXPIRED",
        lastActiveAt: now,
        message: "Session expired. Unlock again to continue.",
      };

    case "REQUEST_RESET":
      return {
        ...session,
        state: "RESET_PENDING",
        lastActiveAt: now,
        message: "Confirm destructive wallet reset.",
      };

    case "CANCEL_RESET":
      return {
        ...session,
        state: "WALLET_UNLOCKED",
        lastActiveAt: now,
      };

    case "CONFIRM_RESET":
      return {
        state: "NO_WALLET",
        lastActiveAt: now,
        message: "Preview wallet reset complete.",
      };

    default:
      return session;
  }
}

export function isWalletUsable(session: SecuritySession): boolean {
  return session.state === "WALLET_UNLOCKED" || session.state === "REVEAL_SEED_PENDING" || session.state === "RESET_PENDING";
}
