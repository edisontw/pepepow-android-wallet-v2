import type { SecurityStore } from "./securityTypes";

// Phase 5 preview-only mock.
// This file intentionally does not implement production encryption.
// Native Android must replace this with a Keystore-backed implementation.

let previewWalletExists = false;
let previewLocked = true;
let previewLastActiveAt = Date.now();

export const previewSecurityStore: SecurityStore = {
  async hasWallet() {
    return previewWalletExists;
  },

  async lock() {
    previewLocked = true;
  },

  async unlockWithPin(pin: string) {
    const accepted = pin.trim().length >= 4;
    if (accepted) {
      previewWalletExists = true;
      previewLocked = false;
      previewLastActiveAt = Date.now();
    }
    return accepted;
  },

  async unlockWithBiometric() {
    previewWalletExists = true;
    previewLocked = false;
    previewLastActiveAt = Date.now();
    return true;
  },

  markSessionActive() {
    previewLastActiveAt = Date.now();
  },

  async clearWallet() {
    previewWalletExists = false;
    previewLocked = true;
    previewLastActiveAt = Date.now();
  },
};

export function getPreviewSecurityDebugState() {
  return {
    walletExists: previewWalletExists,
    locked: previewLocked,
    lastActiveAt: previewLastActiveAt,
  };
}
