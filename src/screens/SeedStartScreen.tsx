import { AlertTriangle } from "lucide-react";

type SeedStartScreenProps = {
  words: string[];
  error?: string;
  onCreate: () => void;
  onRestore: () => void;
};

export function SeedStartScreen(props: SeedStartScreenProps) {
  return (
    <div className="min-h-screen bg-[#eef7e9] p-4 font-sans text-slate-900">
      <div className="mx-auto max-w-md space-y-4">
        <div className="rounded-3xl bg-white p-6 shadow-sm">
          <div className="mb-2 font-mono text-sm font-bold tracking-[0.25em] text-green-800">🐸 PEPEW WALLET</div>
          <h1 className="text-2xl font-black text-slate-900">Phase 4 Restore Preview</h1>
          <p className="mt-2 text-sm leading-6 text-slate-500">
            Browser-preview wallet for small test funds only. Recovery and signing logic stays local, but this is not production wallet code.
          </p>
        </div>

        <div className="rounded-3xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
          <div className="mb-2 flex items-center gap-2 font-bold"><AlertTriangle size={16} /> Test wallet warning</div>
          <p>Do not use large balances. This prototype uses simplified deterministic derivation for AI Studio testing.</p>
        </div>

        <div className="rounded-3xl bg-white p-5 shadow-sm">
          <div className="mb-3 font-mono text-xs font-bold tracking-widest text-slate-400">DEMO WORDS</div>
          <div className="grid grid-cols-3 gap-2">
            {props.words.map((word, index) => (
              <div key={`${word}-${index}`} className="rounded-xl bg-green-50 p-2 font-mono text-xs">
                <span className="text-slate-400">{index + 1}.</span> {word}
              </div>
            ))}
          </div>
        </div>

        {props.error && <div className="rounded-2xl bg-red-50 p-3 text-sm text-red-700">{props.error}</div>}

        <button onClick={props.onCreate} className="w-full rounded-2xl bg-green-700 py-4 font-mono text-sm font-bold tracking-widest text-white shadow-lg">
          CREATE / OPEN TEST WALLET
        </button>
        <button onClick={props.onRestore} className="w-full rounded-2xl bg-white py-3 font-mono text-xs font-bold tracking-widest text-green-700 shadow-sm">
          RESTORE TEST WALLET
        </button>
      </div>
    </div>
  );
}
