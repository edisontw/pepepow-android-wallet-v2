import { AlertTriangle, Eye, EyeOff } from "lucide-react";
import { useState } from "react";

type SensitiveRevealCardProps = {
  title: string;
  description: string;
  value: string;
  revealLabel?: string;
};

export function SensitiveRevealCard({ title, description, value, revealLabel = "REVEAL" }: SensitiveRevealCardProps) {
  const [requested, setRequested] = useState(false);
  const [visible, setVisible] = useState(false);

  if (visible) {
    return (
      <section className="rounded-3xl border border-red-100 bg-white p-5 shadow-sm">
        <button
          onClick={() => setVisible(false)}
          className="flex w-full items-center justify-between font-mono text-xs font-bold text-red-700"
        >
          Hide {title} <EyeOff size={14} />
        </button>
        <div className="mt-3 break-all rounded-2xl bg-red-50 p-3 font-mono text-xs text-red-800">{value}</div>
      </section>
    );
  }

  if (requested) {
    return (
      <section className="rounded-3xl border border-red-100 bg-white p-5 shadow-sm">
        <div className="mb-2 flex items-center gap-2 text-sm font-bold text-red-700">
          <AlertTriangle size={16} /> Sensitive data warning
        </div>
        <p className="text-xs leading-5 text-red-800">{description}</p>
        <button
          onClick={() => setVisible(true)}
          className="mt-3 w-full rounded-2xl bg-red-600 py-3 font-mono text-xs font-bold tracking-widest text-white"
        >
          I UNDERSTAND, {revealLabel}
        </button>
        <button
          onClick={() => setRequested(false)}
          className="mt-2 w-full rounded-2xl bg-white py-3 font-mono text-xs font-bold tracking-widest text-green-700 shadow-sm"
        >
          CANCEL
        </button>
      </section>
    );
  }

  return (
    <section className="rounded-3xl border border-red-100 bg-white p-5 shadow-sm">
      <button
        onClick={() => setRequested(true)}
        className="flex w-full items-center justify-between font-mono text-xs font-bold text-red-700"
      >
        Request {title} Reveal <Eye size={14} />
      </button>
    </section>
  );
}
