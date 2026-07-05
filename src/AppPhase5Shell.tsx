import { useCallback, useReducer } from "react";
import { Lock } from "lucide-react";
import App from "./App";
import { LockScreen } from "./screens/LockScreen";
import { initialSecuritySession, isWalletUsable, securityReducer } from "./security/securityReducer";
import { previewSecurityStore } from "./security/securityStore";
import { useAutoLock } from "./security/useAutoLock";

export default function AppPhase5Shell() {
  const [security, dispatchSecurity] = useReducer(securityReducer, {
    ...initialSecuritySession,
    state: "WALLET_UNLOCKED",
  });

  const lockPreview = useCallback(() => {
    previewSecurityStore.lock();
    dispatchSecurity({ type: "LOCK" });
  }, []);

  useAutoLock({
    enabled: security.state === "WALLET_UNLOCKED",
    lastActiveAt: security.lastActiveAt,
    onLock: lockPreview,
  });

  if (!isWalletUsable(security)) {
    return (
      <LockScreen
        message={security.message ?? "Phase 5 preview shell locked the wallet UI."}
        onUnlock={(pin) => {
          if (pin.trim().length >= 4) dispatchSecurity({ type: "UNLOCK" });
        }}
        onBiometricUnlock={() => dispatchSecurity({ type: "UNLOCK" })}
        onReset={() => dispatchSecurity({ type: "REQUEST_RESET" })}
      />
    );
  }

  return (
    <div>
      <div className="fixed bottom-4 right-4 z-50 flex flex-col items-end gap-2">
        <div className="rounded-2xl bg-slate-900 px-3 py-2 font-mono text-[10px] font-bold tracking-widest text-white shadow-lg">
          PHASE 5 SECURITY SHELL
        </div>
        <button
          onClick={lockPreview}
          className="flex items-center gap-2 rounded-full bg-green-700 px-4 py-3 font-mono text-xs font-bold text-white shadow-lg"
        >
          <Lock size={14} /> LOCK
        </button>
      </div>
      <App />
    </div>
  );
}
