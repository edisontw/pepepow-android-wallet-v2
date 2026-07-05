import { useCallback, useReducer } from "react";
import { Lock } from "lucide-react";
import App from "./App";
import { LockScreen } from "./screens/LockScreen";
import { ResetConfirmCard } from "./screens/ResetConfirmCard";
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

  const confirmReset = useCallback(() => {
    previewSecurityStore.clearWallet();
    dispatchSecurity({ type: "CONFIRM_RESET" });
  }, []);

  useAutoLock({
    enabled: security.state === "WALLET_UNLOCKED",
    lastActiveAt: security.lastActiveAt,
    onLock: lockPreview,
  });

  if (security.state === "RESET_PENDING") {
    return (
      <main className="mx-auto flex min-h-screen max-w-md flex-col justify-center space-y-4 bg-[#eef7e9] p-4 text-slate-900">
        <ResetConfirmCard
          title="Reset wallet"
          description="This clears the local wallet session. Type RESET to continue."
          onConfirm={confirmReset}
          onCancel={() => dispatchSecurity({ type: "CANCEL_RESET" })}
        />
      </main>
    );
  }

  if (!isWalletUsable(security)) {
    return (
      <LockScreen
        message={security.message ?? "Wallet locked."}
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
