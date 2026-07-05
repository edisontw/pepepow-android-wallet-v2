import { AlertTriangle } from "lucide-react";
import { useState } from "react";

type ResetConfirmCardProps = {
  title?: string;
  description?: string;
  onConfirm: () => void;
  onCancel?: () => void;
};

export function ResetConfirmCard({
  title = "Reset preview wallet",
  description = "This clears the local browser-preview wallet state. Type RESET to continue.",
  onConfirm,
  onCancel,
}: ResetConfirmCardProps) {
  const [text, setText] = useState("");

  return (
    <section className="rounded-3xl border border-red-200 bg-white p-5 shadow-sm">
      <div className="mb-2 flex items-center gap-2 font-mono text-xs font-bold tracking-widest text-red-700">
        <AlertTriangle size={16} /> {title.toUpperCase()}
      </div>
      <p className="text-sm leading-6 text-slate-600">{description}</p>
      <input
        value={text}
        onChange={(event) => setText(event.target.value)}
        className="mt-4 w-full rounded-xl border border-red-100 p-3 font-mono text-sm outline-none focus:border-red-500"
        placeholder="RESET"
      />
      <button
        disabled={text !== "RESET"}
        onClick={onConfirm}
        className="mt-4 w-full rounded-2xl bg-red-600 py-4 font-mono text-xs font-bold tracking-widest text-white disabled:bg-slate-300"
      >
        CONFIRM RESET
      </button>
      {onCancel && (
        <button
          onClick={onCancel}
          className="mt-3 w-full rounded-2xl bg-white py-3 font-mono text-xs font-bold tracking-widest text-green-700 shadow-sm"
        >
          CANCEL
        </button>
      )}
    </section>
  );
}
