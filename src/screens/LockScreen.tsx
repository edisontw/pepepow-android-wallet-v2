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
  const [localError, setLocalError] = useState("");

  function submitPin() {
    if (pin.trim().length < 4) {
      setLocalError("Enter your wallet PIN.");
      return;
    }
    setLocalError("");
    onUnlock(pin);
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-md flex-col justify-center space-y-4 bg-[#eef7e9] p-4 text-slate-900">
      <section className="rounded-3xl bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center gap-3 text-green-800">
          <div className="rounded-2xl bg-green-50 p-3"><Lock size={22} /></div>
          <div>
            <div className="font-mono text-xs font-black tracking-widest">PEPEW WALLET LOCKED</div>
            <div className="text-xs text-slate-500">Unlock to continue</div>
          </div>
        </div>

        {message && <div className="mb-4 rounded-2xl bg-amber-50 p-3 text-xs leading-5 text-amber-800">{message}</div>}
        {localError && <div className="mb-4 rounded-2xl bg-red-50 p-3 text-xs leading-5 text-red-700">{localError}</div>}

        <label className="font-mono text-xs font-bold tracking-widest text-slate-500">PIN</label>
        <input
          value={pin}
          onChange={event => {
            setPin(event.target.value);
            setLocalError("");
          }}
          onKeyDown={event => {
            if (event.key === "Enter") submitPin();
          }}
          className="mt-2 w-full rounded-xl border border-green-100 p-3 font-mono text-sm outline-none focus:border-green-500"
          placeholder="Enter PIN"
          type="password"
        />

        <button
          onClick={submitPin}
          className="mt-4 w-full rounded-2xl bg-green-700 py-4 font-mono text-xs font-bold tracking-widest text-white"
        >
          UNLOCK WALLET
        </button>

        <button
          onClick={onBiometricUnlock}
          className="mt-3 flex w-full items-center justify-center gap-2 rounded-2xl bg-white py-3 font-mono text-xs font-bold tracking-widest text-green-700 shadow-sm"
        >
          <ShieldCheck size={14} /> USE BIOMETRIC UNLOCK
        </button>
      </section>

      <section className="rounded-3xl border border-green-100 bg-white p-5 text-xs leading-5 text-green-900 shadow-sm">
        Your wallet remains non-custodial. Recovery words and private keys stay on this device.
      </section>

      <button onClick={onReset} className="rounded-2xl bg-red-600 py-4 font-mono text-xs font-bold tracking-widest text-white">
        RESET WALLET
      </button>
    </main>
  );
}
