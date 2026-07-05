# PEPEPOW Wallet Parameters

This document records the PEPEPOW network and wallet constants used by PEPEW Wallet.

## Network parameters

| Parameter | Value | Notes |
| --- | ---: | --- |
| `port` | `8833` | PEPEPOW P2P network port |
| `packetMagic` | `0xbf0c6bbd` | PEPEPOW network magic bytes |

## Address and key headers

| Parameter | Decimal | Hex | Notes |
| --- | ---: | ---: | --- |
| `addressHeader` | `55` | `0x37` | P2PKH / pubKeyHash address header |
| `pubKeyHash` | `55` | `0x37` | Same value as `addressHeader` |
| `p2shHeader` | `16` | `0x10` | P2SH address header |
| `dumpedPrivateKeyHeader` | `204` | `0xCC` | WIF private key header |
| `wif` | `204` | `0xCC` | Same value as `dumpedPrivateKeyHeader` |

## HD wallet parameters

| Parameter | Value | Notes |
| --- | --- | --- |
| `coinType` | `5` | SLIP-44 / BIP44 coin type used by this wallet |
| `defaultPath` | `m/44'/5'/0'/0/0` | Default receive address derivation path |

## Wallet derivation rule

PEPEW Wallet should derive the default address using:

```text
BIP39 mnemonic
  -> BIP32 seed / HD node
  -> defaultPath: m/44'/5'/0'/0/0
  -> public key
  -> pubKeyToP2PKH
```

The resulting P2PKH address uses:

```text
addressHeader = 55 = 0x37
```

The exported private key uses WIF with:

```text
dumpedPrivateKeyHeader = 204 = 0xCC
```

## Non-custodial API boundary

The public API should only receive blockchain query and broadcast data:

```text
address
balance/history requests
UTXO requests
signed raw transactions for broadcast
```

The app must not send the following to the API:

```text
mnemonic
seed
private key
WIF
unsigned sensitive wallet material
```
