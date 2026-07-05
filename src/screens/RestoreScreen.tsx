import { AlertTriangle } from "lucide-react";
import { useMemo, useState } from "react";
import { validatePrototypeRestorePhrase } from "../restore/restoreValidation";
import { prototypeWalletDeriver } from "../wallet/walletDeriver";
import { shortText } from "../utils/format";

type RestoreScreenProps = {
  onRestore: (words: string[]) => void;
  onCancel: () => void;
};

export function RestoreScreen(props: RestoreScreenProps) {
  const [input, setInput] = useState("");
  const validation = useMemo(() => validatePrototypeRestorePhrase(input), [input]);
  const preview = useMemo(() => {
    if (!validation.ok) return null;
    const wallet = prototypeWalletDeriver.derivePrimaryWallet(validation.words);
    return wallet.address;
  }, [validation]);

  return (
    <main className="mx-auto max-w-md space-y-4 p-4">
      <section className="rounded-3xl bg-white p-5 shadow-sm">
        <div className="mb-2 font-mono text-xs font-bold tracking-widest text-slate-400">RESTORE TEST WALLET</div>
        <h1 className="text-xl font-black text-slate-900">Restore from 12 words</h1>
        <p className="mt-2 text-sm leading-6 text-slate-500">
          This restores the experimental preview wallet only. It is not production wallet-core recovery.
        </p>
      </section>

      <section className="rounded-3xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
        <div className="mb-2 flex items-center gap-2 font-bold"><AlertTriangle size={16} /> Experimental restore</div>
        <p>Use small test funds only. Do not use this preview as a production recovery tool.</p>
      </section>

      <section className="rounded-3xl bg-white p-5 shadow-sm">
        <label className="font-mono text-xs font-bold tracking-widest text-slate-500">12 WORDS</label>
        <textarea
          value={input}
          onChange={event => setInput(event.target.value)}
          className="mt-2 h-32 w-full rounded-2xl border border-green-100 p-3 font-mono text-xs outline-none focus:border-green-500"
          placeholder="Paste exactly 12 words"
        />
        {!validation.ok && input.trim().length > 0 && (
          <div className="mt-3 rounded-xl bg-red-50 p-3 text-xs text-red-700">{validation.error}</div>
        )}
        {preview && (
          <div className="mt-3 rounded-xl bg-green-50 p-3 font-mono text-xs text-green-900">
            Derived test address: {shortText(preview, 12, 10)}
          </div>
        )}
      </section>

      <button
        disabled={!validation.ok}
        onClick={() => validation.ok && props.onRestore(validation.words)}
        className="w-full rounded-2xl bg-green-700 py-4 font-mono text-xs font-bold tracking-widest text-white disabled:bg-slate-300"
      >
        RESTORE TEST WALLET
      </button>
      <button onClick={props.onCancel} className="w-full rounded-2xl bg-white py-3 font-mono text-xs font-bold tracking-widest text-green-700 shadow-sm">
        CANCEL
      </button>
    </main>
  );
}
