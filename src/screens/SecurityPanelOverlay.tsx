import { ShieldCheck, X } from "lucide-react";
import { SecuritySettingsScreen } from "./SecuritySettingsScreen";
import type { SecurityState } from "../security/securityTypes";

type SecurityPanelOverlayProps = {
  state: SecurityState;
  open: boolean;
  onClose: () => void;
  onLock: () => void;
  onRequestReset: () => void;
};

export function SecurityPanelOverlay({ state, open, onClose, onLock, onRequestReset }: SecurityPanelOverlayProps) {
  if (!open) return null;

  return (
    <div className="w-[min(24rem,calc(100vw-2rem))] rounded-3xl border border-green-100 bg-[#eef7e9] p-3 shadow-2xl">
      <div className="mb-2 flex items-center justify-between px-2 font-mono text-[10px] font-bold tracking-widest text-green-800">
        <span className="flex items-center gap-2"><ShieldCheck size={14} /> PHASE 5 PANEL</span>
        <button onClick={onClose} className="rounded-full bg-white p-1 text-slate-500 shadow-sm">
          <X size={14} />
        </button>
      </div>
      <SecuritySettingsScreen state={state} onLock={onLock} onRequestReset={onRequestReset} />
    </div>
  );
}
