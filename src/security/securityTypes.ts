export type SecurityState =
  | "UNINITIALIZED"
  | "NO_WALLET"
  | "WALLET_LOCKED"
  | "WALLET_UNLOCKED"
  | "REVEAL_SEED_PENDING"
  | "SESSION_EXPIRED"
  | "RESET_PENDING";

export type SecurityAction =
  | { type: "BOOT"; hasWallet: boolean }
  | { type: "CREATE_OR_RESTORE_WALLET" }
  | { type: "LOCK" }
  | { type: "UNLOCK" }
  | { type: "REQUEST_SEED_REVEAL" }
  | { type: "CANCEL_SEED_REVEAL" }
  | { type: "SESSION_EXPIRED" }
  | { type: "REQUEST_RESET" }
  | { type: "CANCEL_RESET" }
  | { type: "CONFIRM_RESET" };

export type UnlockMethod = "PIN_PLACEHOLDER" | "BIOMETRIC_PLACEHOLDER";

export interface SecuritySession {
  state: SecurityState;
  lastActiveAt: number;
  unlockMethod?: UnlockMethod;
  message?: string;
}

export interface SecurityStore {
  hasWallet(): Promise<boolean>;
  lock(): Promise<void>;
  unlockWithPin(pin: string): Promise<boolean>;
  unlockWithBiometric(): Promise<boolean>;
  markSessionActive(): void;
  clearWallet(): Promise<void>;
}
