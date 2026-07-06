# PEPEW Wallet

PEPEW Wallet is a non-custodial PEPEPOW wallet application.

- App name: `PEPEW Wallet`
- Android package: `net.pepepow.wallet`
- Public API: `https://light.pepepow.net/`
- Wallet rule: `BIP39 -> BIP32 -> m/44'/5'/0'/0/0 -> pubkeyToP2PKH`

## Non-custodial boundary

The app keeps recovery words, seed material, private keys, and signing logic local.

The PEPEW Light API is used for:

```text
address balance/history lookup
UTXO lookup
signed raw transaction broadcast
```

The API must not receive:

```text
mnemonic
seed
private key
WIF
unsigned sensitive wallet material
```

## Documentation

Project documentation is collected in [`docs/`](./docs/).

Key documents:

- [`docs/pepepow-parameters.md`](./docs/pepepow-parameters.md) — PEPEPOW network, address, WIF, and HD wallet constants.
- [`docs/android-icon-setup.md`](./docs/android-icon-setup.md) — Android launcher icon setup.
- [`docs/release-polish-status.md`](./docs/release-polish-status.md) — UI polish and verification status.

## PEPEPOW parameters

| Parameter | Value |
| --- | --- |
| `addressHeader` / `pubKeyHash` | `55` / `0x37` |
| `dumpedPrivateKeyHeader` / `wif` | `204` / `0xCC` |
| `p2shHeader` | `16` / `0x10` |
| `coinType` | `5` |
| `defaultPath` | `m/44'/5'/0'/0/0` |
| `port` | `8833` |
| `packetMagic` | `0xbf0c6bbd` |

## Development

```bash
npm install
npm run lint
npm run build
npm run dev
```

## Android build notes

The Android launcher icon currently uses:

```xml
android:icon="@drawable/pepew_logo"
android:roundIcon="@drawable/pepew_logo"
```

The logo resource is:

```text
app/src/main/res/drawable/pepew_logo.png
```

## Android Development & Testing

The Android project is located in the `app` folder.

To build the debug APK:
```powershell
.\gradlew.bat :app:assembleDebug
```

To run unit tests:
```powershell
.\gradlew.bat :app:testDebugUnitTest
```

For detailed implementation status, see [`docs/wallet-implementation-status.md`](./docs/wallet-implementation-status.md).
