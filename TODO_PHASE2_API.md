# TODO Phase 2 API

Phase 2 goal: add **read-only PEPEW Light API integration** while keeping wallet secrets local and avoiding any signing/broadcast logic.

Endpoint baseline:

```text
https://light.pepepow.net/
```

## Phase 2 Boundaries

Phase 2 may add:

- API health/status lookup.
- Balance lookup by address.
- Transaction history lookup by address.
- Error/loading/retry states.
- Repository switch from fake-only to a controlled read-only API repository.
- Basic address input/format validation before API requests.

Phase 2 must not add:

- Real mnemonic generation.
- Seed encryption or persistence.
- Private key derivation.
- Address derivation from seed.
- UTXO selection.
- Transaction building.
- Transaction signing.
- Transaction broadcast.
- Sending mnemonic, seed, private key, or signed transaction data to any backend.

## Proposed Phase 2 Architecture

```text
UI
  -> ViewModel
    -> WalletRepository interface
      -> FakeWalletRepository       // keep for previews/offline mode/tests
      -> ReadOnlyApiWalletRepository // new in Phase 2
        -> PepewApiClient            // real read-only HTTP client
```

## API State Model

Current Phase 1 states are simulated:

```text
CONNECTED
READY
FAILED
```

For Phase 2, consider expanding to:

```text
API_CONNECTED
API_READY
API_FAILED_TRANSIENT
API_DISABLED
```

Suggested mapping:

| State | Meaning |
| --- | --- |
| `API_CONNECTED` | HTTP endpoint reachable, but wallet data readiness not yet confirmed. |
| `API_READY` | Endpoint reachable and address balance/history lookups are working. |
| `API_FAILED_TRANSIENT` | Temporary failure, timeout, rate limit, or network issue. Retry allowed. |
| `API_DISABLED` | User/app disabled API mode or endpoint is intentionally unavailable. |

## Implementation Checklist

### 1. API Client

- Replace placeholder methods in `PepewApiClient` with read-only HTTP methods only.
- Add request timeout.
- Add typed response models.
- Parse error response bodies safely.
- Do not add broadcast methods yet, or keep broadcast explicitly unsupported.

Suggested methods:

```kotlin
suspend fun getHealth(): ApiHealth
suspend fun getStatus(): ApiStatus
suspend fun getBalance(address: String): ApiBalance
suspend fun getHistory(address: String): List<ApiTransaction>
```

### 2. Repository

Create `ReadOnlyApiWalletRepository` only after `PepewApiClient` is tested.

Responsibilities:

- Hold active address.
- Fetch balance by address.
- Fetch transaction history by address.
- Convert API DTOs into UI models.
- Expose loading and error states.
- Never access mnemonic/private key material.

### 3. UI / ViewModel

- Add loading indicators for balance/history.
- Add retry action for transient API failure.
- Keep mock mode available for development.
- Keep Send behavior fake or disabled until Phase 3.

### 4. Error Handling

Handle at least:

- No internet.
- API unavailable.
- API timeout.
- Invalid address.
- Empty history.
- Rate limited response.
- Malformed API response.

### 5. Testing

Add unit tests for:

- Address validation.
- API DTO parsing.
- Repository state transitions.
- Empty balance/history states.
- Transient API failure handling.

Manual smoke test:

```bash
./gradlew assembleDebug
```

Then verify in Android Studio/device:

- App opens.
- Mock wallet flow still works.
- API status screen can show read-only API state.
- Balance/history loading does not block UI.
- No real send/broadcast path exists.

## Migration Rule

Before merging Phase 2 code, confirm:

```text
grep -R "mnemonic\|privateKey\|seed\|sign\|broadcast\|utxo" app/src/main/java
```

Any match must be reviewed to ensure it is either:

1. mock-only text, or
2. a placeholder explicitly not wired into runtime.
