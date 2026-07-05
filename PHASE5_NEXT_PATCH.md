# Phase 5 Next Patch Guide

This guide keeps the next implementation step small and avoids replacing the large `src/App.tsx` file.

## Already added

```text
src/AppPhase5Shell.tsx
src/screens/LockScreen.tsx
src/screens/ResetConfirmCard.tsx
src/screens/SensitiveRevealCard.tsx
src/screens/SecurityPanelOverlay.tsx
src/screens/Phase5StatusBadge.tsx
src/security/securityTypes.ts
src/security/securityReducer.ts
src/security/securityStore.ts
src/security/useAutoLock.ts
src/security/securityReducer.selftest.ts
```

## Safe next patch

Modify only `src/AppPhase5Shell.tsx`.

Goal:

- Show a SECURITY button next to LOCK.
- Toggle `SecurityPanelOverlay` from the shell.
- Replace the hardcoded shell label with `Phase5StatusBadge`.

Suggested minimal steps:

1. Import `useState` from React.
2. Import `ShieldCheck` from `lucide-react`.
3. Import `SecurityPanelOverlay`.
4. Import `Phase5StatusBadge`.
5. Add `const [panelOpen, setPanelOpen] = useState(false);`.
6. Render `SecurityPanelOverlay` above the floating buttons.
7. Add a SECURITY button that toggles `panelOpen`.
8. Keep the existing LOCK button unchanged.

## After that

Only after the shell panel works, move deeper integration into extracted screens:

```text
src/screens/ReceiveScreen.tsx
src/screens/SettingsScreen.tsx
```

Then replace:

- direct WIF reveal with `SensitiveRevealCard`
- direct reset button with `ResetConfirmCard`

## Build check

Run:

```bash
npm install
npm run lint
npm run build
```

## Boundary

Phase 5 is still preview security scaffolding. Do not claim production encrypted storage until native Android Keystore-backed storage is implemented and reviewed.
