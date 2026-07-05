# Phase 2 Status

Status: **implemented baseline, needs device/API smoke test**

## What changed

Phase 2 switches the active runtime repository from mock-only data to a read-only PEPEW Light API repository:

```text
MainActivity
  -> ReadOnlyApiWalletRepository
    -> PepewApiClient
      -> https://light.pepepow.net/
```

The mock repository remains in the project for previews, fallback checks, and Phase 1 comparison.

## Implemented

- Real read-only HTTP client using Android/JDK networking APIs.
- API health lookup: `GET /api/health`.
- API status lookup: `GET /api/status`.
- Address summary lookup: `GET /api/wallet/address/{address}`.
- Address history lookup: `GET /api/wallet/history/{address}?limit=50&offset=0`.
- Safe JSON parsing with flexible field names for early API response changes.
- Loading and API message state at repository/ViewModel level.
- Runtime repository changed to `ReadOnlyApiWalletRepository`.
- Send remains disabled in read-only API mode.
- Broadcast is explicitly unsupported in the Android Phase 2 client.

## Security boundary

Phase 2 still does **not** implement:

- Real mnemonic generation.
- Seed persistence.
- Seed encryption.
- Private key derivation.
- Address derivation from seed.
- UTXO selection.
- Transaction building.
- Transaction signing.
- Real broadcast.

The API client only sends public address/status requests.

## Demo address

Because real address derivation is intentionally not implemented yet, Phase 2 uses a public documentation example address for read-only API testing:

```text
PRfbEeHAKKbz6Voz85WJudrJwTA3ZbHunb
```

This must be replaced by real locally derived addresses only in later phases after cryptographic wallet logic is implemented and reviewed.

## Manual test checklist

Run in Android Studio or from shell:

```bash
./gradlew assembleDebug
```

Then verify:

- App opens.
- Create wallet flow still works.
- Dashboard shows the Phase 2 demo address balance from API.
- History screen shows API history or a clean empty state.
- API Status reaches READY when `https://light.pepepow.net/` is reachable.
- Send does not broadcast and shows read-only mode behavior.

## Known UI limitation

The UI still mostly uses the Phase 1 mock layout. Phase 2 focused on repository/API wiring first. A later polish pass should improve visible loading/error labels on Dashboard, History, and API Status.
