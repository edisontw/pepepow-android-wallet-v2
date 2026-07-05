export type Screen = "seed" | "dashboard" | "receive" | "send" | "history" | "settings";

export type ApiState = "CONNECTED" | "READY" | "FAILED";

export type Tx = {
  id: string;
  amount: number;
  address: string;
  timestamp: number;
  isSend: boolean;
  isPending?: boolean;
  isUnknownAmount?: boolean;
};

export type BroadcastResult = {
  success: boolean;
  txid?: string;
  error?: string;
};

export type SendReview = {
  from: string;
  to: string;
  amount: number;
  fee: number;
  totalSpent: number;
  inputTotal: number;
  change: number;
  inputs: number;
};

export type WalletApiSnapshot = {
  balance: number;
  txs: Tx[];
  utxoCount: number;
  utxoTotal: number;
  height: string;
  apiMessage: string;
  apiState: ApiState;
};
