import type { RestoreScanSummary } from "./restoreTypes";
import type { WalletDeriver } from "../wallet/walletDeriver";

export type WalletScannerOptions = {
  gapLimit: number;
};

export async function scanPrototypeWallet(
  words: string[],
  deriver: WalletDeriver,
  _options: WalletScannerOptions = { gapLimit: 1 },
): Promise<RestoreScanSummary> {
  const receive = deriver.deriveReceiveAddress(words, 0);
  const change = deriver.deriveChangeAddress(words, 0);
  const uniqueAddresses = Array.from(new Set([receive.address, change.address]));

  return {
    receiveAddressesChecked: 1,
    changeAddressesChecked: 1,
    usedAddresses: uniqueAddresses,
    totalBalance: 0,
    txCount: 0,
  };
}
