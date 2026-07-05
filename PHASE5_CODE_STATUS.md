# Phase 5 Code Status

Phase 5 has started as isolated UI and state scaffold.

## Added files

```text
src/security/securityTypes.ts
src/security/securityReducer.ts
src/security/securityStore.ts
src/security/useAutoLock.ts
src/screens/LockScreen.tsx
src/screens/SecuritySettingsScreen.tsx
```

## Implemented

- App security state names
- Security reducer scaffold
- Preview-only mock security store
- Auto-lock hook scaffold
- Lock screen component
- Security settings component

## Not wired yet

`src/App.tsx` still needs a small follow-up patch to import and use the new scaffold.

Required follow-up:

- add reducer state in `App.tsx`
- call the create/restore unlock action after wallet creation or restore
- add manual lock button in Settings
- show `LockScreen` when wallet is locked
- show `SecuritySettingsScreen` in Settings
- add confirmation before WIF reveal
- add typed confirmation before local preview wallet reset

## Acceptance checklist

- [x] Security state model added.
- [x] Reducer scaffold added.
- [x] Mock security store added.
- [x] Auto-lock hook scaffold added.
- [x] Lock screen component added.
- [x] Security settings component added.
- [ ] App wiring completed.
- [ ] Build verified.

## Boundary

This is preview-only scaffolding. Do not claim production encrypted storage yet. Native Android Keystore-backed storage remains a later step.
