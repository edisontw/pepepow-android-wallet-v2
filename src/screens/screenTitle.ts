import type { Screen } from "../types/wallet";

export function screenTitle(screen: Screen): string {
  switch (screen) {
    case "receive":
      return "Receive PEPEW";
    case "send":
      return "Send PEPEW";
    case "history":
      return "Transaction History";
    case "settings":
      return "Wallet Configuration";
    case "restore":
      return "Restore Test Wallet";
    case "dashboard":
    case "seed":
    default:
      return "PEPEW Wallet";
  }
}
