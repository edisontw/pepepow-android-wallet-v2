import { useCallback, useReducer, useState } from "react";
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
  const [resetText, setResetText] = useState("");

  const lockPreview = useCallback(() => {
    previewSecurityStore.lock();
    dispatchSecurity({ type: "LOCK" });
  }, []);

  const confirmReset = useCallback(() => {
    previewSecurityStore.clearWallet();
    setResetText("");
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
        <section className="rounded-3xl border border-red-200 bg-white p-6 shadow-sm">
          <div className="mb-2 font-mono text-xs font-bold tracking-widest text-red-700">RESET PREVIEW WALLET</div>
          <p className="text-sm leading-6 text-slate-600">
            This clears the Phase 5 shell session state for the browser preview. Type RESET to continue.
          </p>
          <input
            value={resetText}
            onChange={(event) => setResetText(event.target.value)}
            className="mt-4 w-full rounded-xl border border-red-100 p-3 font-mono text-sm outline-none focus:border-red-500"
            placeholder="RESET"
          />
          <button
            disabled={resetText !== "RESET"}
            onClick={confirmReset}
            className="mt-4 w-full rounded-2xl bg-red-600 py-4 font-mono text-xs font-bold tracking-widest text-white disabled:bg-slate-300"
          >
            CONFIRM RESET
          </button>
          <button
            onClick={() => dispatchSecurity({ type: "CANCEL_RESET" })}
            className="mt-3 w-full rounded-2xl bg-white py-3 font-mono text-xs font-bold tracking-widest text-green-700 shadow-sm"
          >
            CANCEL
          </button>
        </section>
      </main>
    );
  }

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
