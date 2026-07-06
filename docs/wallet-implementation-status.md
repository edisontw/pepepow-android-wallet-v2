# PEPEPOW Wallet Implementation Status

This document records the verification results, implemented features, and security boundary compliance for the PEPEW Android Wallet.

## Implementation Details

### What is Implemented
* **BIP39 Mnemonic Service:** Generates cryptographically secure 12-word recovery phrases, parses and normalizes inputs, and checks checksum bytes.
* **BIP32 HD Derivation:** Derives seed phrases and child keys deterministically at the default path `m/44'/5'/0'/0/0`.
* **Base58Check Encoding/Decoding:** Handles network addresses and private keys (WIF) with checksum integrity.
* **Local Transaction Signer:** Constructs outputs, performs UTXO input matching, computes double-SHA-256 transaction hashes, and signs transactions locally using canonical low-S ECDSA signatures (RFC 6979).
* **Keystore-Backed Secure Storage:** Encrypts mnemonic words and derived addresses using Android `EncryptedSharedPreferences`.
* **Pepepow Light API Integration:** Connected to endpoints for balance check, history sync, spendable UTXOs query, and raw signed transaction broadcast.

### What is Verified
* All cryptographic operations, key derivation paths, base58 encoding, and address validators are tested and verified via a comprehensive unit test suite (`WalletUnitTest`).

### Source of PEPEPOW Parameters
The network and wallet parameters are extracted and verified from the TypeScript web preview wallet code located in this repository (`src/wallet/pepepowParams.ts` and `src/wallet/walletUtils.ts`):
* `addressHeader` (P2PKH) = 55 (0x37)
* `p2shHeader` (P2SH) = 16 (0x10)
* `wif` (dumpedPrivateKeyHeader) = 204 (0xCC)
* `coinType` = 5
* `defaultPath` = `m/44'/5'/0'/0/0`
* `lightApiBase` = `https://light.pepepow.net/`

---

## Technical Status

* **Real Send Enabled:** Yes. Users can build, sign, and broadcast transactions from their phone.
* **Blocked Parts:** None. All parameters, endpoints, and transaction serialization structures are fully verified.

---

## API Endpoints Used

The app connects to the production Light API node at `https://light.pepepow.net/` using only public data:
* **Health Check:** `GET /api/health`
* **Sync Height:** `GET /api/status`
* **Address Summary:** `GET /api/wallet/address/{address}`
* **History Sync:** `GET /api/wallet/history/{address}`
* **UTXO Query:** `GET /api/wallet/utxo/{address}`
* **Tx Broadcast:** `POST /api/wallet/broadcast` (Payload: `{"raw_tx": "<hex>"}`)

---

## Security Boundary Checklist

- [x] **Private keys, seeds, or mnemonics never leave the device:** Keys are derived on-demand and kept in RAM during signing. No secret material is ever sent to the Light API.
- [x] **Secure Storage Encryption:** Plain SharedPreferences storage is wiped. Mnemonics are written exclusively to `EncryptedSharedPreferences` backed by the Android Keystore.
- [x] **No Logs Expose Secrets:** Logging does not print recovery phrases, seeds, or private key hashes.
- [x] **Broadcast isolation:** Broadcast sends only the fully signed raw transaction hex.
