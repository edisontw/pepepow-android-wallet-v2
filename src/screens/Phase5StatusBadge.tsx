import { ShieldCheck } from "lucide-react";

type Phase5StatusBadgeProps = {
  state?: string;
};

export function Phase5StatusBadge({ state = "SECURITY SCAFFOLD" }: Phase5StatusBadgeProps) {
  return (
    <div className="rounded-2xl bg-slate-900 px-3 py-2 font-mono text-[10px] font-bold tracking-widest text-white shadow-lg">
      <span className="inline-flex items-center gap-2">
        <ShieldCheck size={14} /> PHASE 5 · {state}
      </span>
    </div>
  );
}
