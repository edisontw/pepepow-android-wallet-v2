export type RestoreValidationResult = {
  ok: boolean;
  words: string[];
  error?: string;
};

export type RestorePreview = {
  words: string[];
  address: string;
  warning: string;
};

export type RestoreScanState = "idle" | "validating" | "scanning" | "complete" | "failed";

export type RestoreScanSummary = {
  receiveAddressesChecked: number;
  changeAddressesChecked: number;
  usedAddresses: string[];
  totalBalance: number;
  txCount: number;
};
