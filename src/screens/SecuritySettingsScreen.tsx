import React, { useState } from "react";
import { AlertTriangle, Lock, ShieldCheck } from "lucide-react";
import type { SecurityState } from "../security/securityTypes";

type SecuritySettingsScreenProps = {
  state: SecurityState;
  onLock: () => void;
  onRequestReset: () => void;
};

export function SecuritySettingsScreen({ state, onLock, onRequestReset }: SecuritySettingsScreenProps) {
  const [autoLockEnabled, setAutoLockEnabled] = useState(true);

  return (
    <section className="rounded-3xl bg-white p-5 shadow-sm">
      <div className="mb-4 flex items-center gap-2 font-mono text-xs font-bold tracking-widest text-green-700">
        <ShieldCheck size={16} /> PHASE 5 SECURITY SCAFFOLD
      </div>

      <div className="space-y-3 text-sm text-slate-700">
        <div className="flex justify-between border-b border-slate-100 pb-3">
          <span className="font-mono text-xs text-slate-400">Lock state</span>
          <b className="font-mono text-xs text-green-800">{state}</b>
        </div>

        <label className="flex items-center justify-between gap-3 rounded-2xl bg-green-50 p-3">
          <span className="text-xs leading-5 text-green-900">Auto-lock scaffold enabled</span>
          <input type="checkbox" checked={autoLockEnabled} onChange={event => setAutoLockEnabled(event.target.checked)} />
        </label>

        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-3 text-xs leading-5 text-amber-800">
          <div className="mb-1 flex items-center gap-2 font-bold"><AlertTriangle size={14} /> Preview only</div>
          This does not provide audited production encrypted storage. Android Keystore-backed storage is still required.
        </div>
      </div>

      <button onClick={onLock} className="mt-4 flex w-full items-center justify-center gap-2 rounded-2xl bg-slate-900 py-4 font-mono text-xs font-bold tracking-widest text-white">
        <Lock size={14} /> LOCK WALLET
      </button>

      <button onClick={onRequestReset} className="mt-3 w-full rounded-2xl bg-red-600 py-4 font-mono text-xs font-bold tracking-widest text-white">
        REQUEST WALLET RESET
      </button>
    </section>
  );
}
