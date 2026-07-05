import {
  derivePrivateKeyFromMnemonic,
  getAddressFromPrivateKey,
  privateKeyToWIF,
} from "./walletUtils";

export type DerivedWalletAddress = {
  address: string;
  pathLabel: string;
  index: number;
  isChange: boolean;
};

export type PrototypeDerivedWallet = {
  mnemonic: string;
  privateKey: Uint8Array;
  address: string;
  wif: string;
};

export interface WalletDeriver {
  derivePrimaryWallet(words: string[]): PrototypeDerivedWallet;
  deriveReceiveAddress(words: string[], index: number): DerivedWalletAddress;
  deriveChangeAddress(words: string[], index: number): DerivedWalletAddress;
}

export const prototypeWalletDeriver: WalletDeriver = {
  derivePrimaryWallet(words: string[]): PrototypeDerivedWallet {
    const mnemonic = words.join(" ").trim();
    const privateKey = derivePrivateKeyFromMnemonic(mnemonic);
    return {
      mnemonic,
      privateKey,
      address: getAddressFromPrivateKey(privateKey, 55),
      wif: privateKeyToWIF(privateKey, 204),
    };
  },

  deriveReceiveAddress(words: string[], index: number): DerivedWalletAddress {
    const primary = this.derivePrimaryWallet(words);
    return {
      address: primary.address,
      pathLabel: `prototype/receive/${index}`,
      index,
      isChange: false,
    };
  },

  deriveChangeAddress(words: string[], index: number): DerivedWalletAddress {
    const primary = this.derivePrimaryWallet(words);
    return {
      address: primary.address,
      pathLabel: `prototype/change/${index}`,
      index,
      isChange: true,
    };
  },
};
