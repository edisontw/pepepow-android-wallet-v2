import React, { useState } from "react";
import { Lock, ShieldCheck } from "lucide-react";

type LockScreenProps = {
  message?: string;
  onUnlock: (pin: string) => void;
  onBiometricUnlock: () => void;
  onReset: () => void;
};

export function LockScreen({ message, onUnlock, onBiometricUnlock, onReset }: LockScreenProps) {
  const [pin, setPin] = useState("");

  return (
    <main className="mx-auto flex min-h-screen max-w-md flex-col justify-center space-y-4 bg-[#eef7e9] p-4 text-slate-900">
      <section className="rounded-3xl bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center gap-3 text-green-800">
          <div className="rounded-2xl bg-green-50 p-3"><Lock size={22} /></div>
          <div>
            <div className="font-mono text-xs font-black tracking-widest">PEPEW WALLET LOCKED</div>
            <div className="text-xs text-slate-500">Phase 5 preview security scaffold</div>
          </div>
        </div>

        {message && <div className="mb-4 rounded-2xl bg-amber-50 p-3 text-xs leading-5 text-amber-800">{message}</div>}

        <label className="font-mono text-xs font-bold tracking-widest text-slate-500">PLACEHOLDER PIN</label>
        <input
          value={pin}
          onChange={event => setPin(event.target.value)}
          className="mt-2 w-full rounded-xl border border-green-100 p-3 font-mono text-sm outline-none focus:border-green-500"
          placeholder="Enter 4+ digits"
          type="password"
        />

        <button
          onClick={() => onUnlock(pin)}
          className="mt-4 w-full rounded-2xl bg-green-700 py-4 font-mono text-xs font-bold tracking-widest text-white"
        >
          UNLOCK PREVIEW WALLET
        </button>

        <button
          onClick={onBiometricUnlock}
          className="mt-3 flex w-full items-center justify-center gap-2 rounded-2xl bg-white py-3 font-mono text-xs font-bold tracking-widest text-green-700 shadow-sm"
        >
          <ShieldCheck size={14} /> BIOMETRIC PLACEHOLDER
        </button>
      </section>

      <section className="rounded-3xl border border-red-100 bg-white p-5 text-xs leading-5 text-red-800 shadow-sm">
        This preview lock is not production encryption. Native Android must use Keystore-backed encrypted storage before production release.
      </section>

      <button onClick={onReset} className="rounded-2xl bg-red-600 py-4 font-mono text-xs font-bold tracking-widest text-white">
        RESET LOCAL PREVIEW WALLET
      </button>
    </main>
  );
}
