import { AlertTriangle, ShieldCheck } from "lucide-react";

const pepewLogoUrl = new URL("../../app/src/main/res/drawable/pepew_logo.png", import.meta.url).href;

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
          <div className="mb-4 flex items-center gap-4">
            <div className="flex h-14 w-14 items-center justify-center overflow-hidden rounded-2xl bg-green-700 shadow-sm">
              <img
                src={pepewLogoUrl}
                alt="PEPEW Wallet logo"
                className="h-full w-full object-cover"
              />
            </div>
            <div>
              <div className="mb-2 font-mono text-sm font-bold tracking-[0.25em] text-green-800">PEPEW WALLET</div>
              <h1 className="text-2xl font-black text-slate-900">Create or restore your wallet</h1>
            </div>
          </div>
          <p className="mt-2 text-sm leading-6 text-slate-500">
            A non-custodial PEPEW wallet. Recovery words and signing stay on this device.
          </p>
        </div>

        <div className="rounded-3xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
          <div className="mb-2 flex items-center gap-2 font-bold"><AlertTriangle size={16} /> Backup required</div>
          <p>Write down your recovery words and keep them private. Anyone with these words can spend your funds.</p>
        </div>

        <div className="rounded-3xl border border-green-200 bg-green-50 p-4 text-sm text-green-900">
          <div className="mb-2 flex items-center gap-2 font-bold"><ShieldCheck size={16} /> Non-custodial</div>
          <p>PEPEW Light API is used for balance, history, UTXO lookup, and transaction broadcast. Private keys are not sent to the API.</p>
        </div>

        {props.error && <div className="rounded-2xl bg-red-50 p-3 text-sm text-red-700">{props.error}</div>}

        <button onClick={props.onCreate} className="w-full rounded-2xl bg-green-700 py-4 font-mono text-sm font-bold tracking-widest text-white shadow-lg">
          CREATE WALLET
        </button>
        <button onClick={props.onRestore} className="w-full rounded-2xl bg-white py-3 font-mono text-xs font-bold tracking-widest text-green-700 shadow-sm">
          RESTORE WALLET
        </button>
      </div>
    </div>
  );
}
