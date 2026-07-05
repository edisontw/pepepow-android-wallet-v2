import React, { useEffect, useMemo, useState } from "react";
import {
  ArrowDownLeft,
  ArrowLeft,
  ArrowUpRight,
  CheckCircle,
  Copy,
  History,
  KeyRound,
  QrCode,
  RefreshCcw,
  Send,
  Settings,
  ShieldCheck,
} from "lucide-react";

type Screen = "seed" | "confirm" | "dashboard" | "receive" | "history" | "send" | "api" | "settings";

type ApiState = "CONNECTED" | "READY" | "FAILED";

type Tx = {
  id: string;
  amount: number;
  address: string;
  timestamp: number;
  isSend: boolean;
  isPending?: boolean;
};

const API_BASE = "https://light.pepepow.net";
const DEMO_ADDRESS = "PRfbEeHAKKbz6Voz85WJudrJwTA3ZbHunb";
const WORDS = ["swamp", "pepe", "key", "power", "wallet", "frog", "meme", "blockchain", "pond", "green", "crypto", "speed"];

function safeText(value: unknown): string {
  if (value === null || value === undefined) return "";
  return String(value);
}

function shortText(value: unknown, head = 8, tail = 6): string {
  const text = safeText(value);
  if (text.length <= head + tail + 3) return text;
  return `${text.slice(0, head)}...${text.slice(-tail)}`;
}

function safeNumber(value: unknown, fallback = 0): number {
  const parsed = typeof value === "number" ? value : Number(safeText(value).replace(/,/g, ""));
  return Number.isFinite(parsed) ? parsed : fallback;
}

function formatAmount(value: unknown, digits = 4): string {
  return safeNumber(value).toLocaleString(undefined, {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  });
}

function formatDate(value: unknown): string {
  const n = safeNumber(value, Date.now());
  const millis = n > 0 && n < 10_000_000_000 ? n * 1000 : n;
  const date = new Date(millis);
  if (Number.isNaN(date.getTime())) return "Unknown date";
  return date.toLocaleString(undefined, { month: "short", day: "2-digit", hour: "2-digit", minute: "2-digit" });
}

function parseApiHistory(items: unknown): Tx[] {
  if (!Array.isArray(items)) return [];
  return items.flatMap((raw, index) => {
    if (!raw || typeof raw !== "object") return [];
    const item = raw as Record<string, unknown>;
    const id = safeText(item.txid ?? item.tx_hash ?? item.hash ?? item.id ?? `tx_${index}`);
    const amountRaw = item.amount_pepew ?? item.value_pepew ?? item.delta_pepew ?? item.amount ?? item.value ?? item.balance_delta;

    // ElectrumX compact history can be txid/height only. Do not render it as +NaN.
    if (amountRaw === undefined || amountRaw === null || amountRaw === "") return [];

    const signedAmount = safeNumber(amountRaw, 0);
    return [{
      id,
      amount: Math.abs(signedAmount),
      address: safeText(item.address ?? id),
      timestamp: safeNumber(item.timestamp ?? item.time, Date.now()),
      isSend: safeText(item.direction).toLowerCase().includes("send") || signedAmount < 0,
      isPending: safeNumber(item.height, 1) <= 0 || item.pending === true,
    }];
  });
}

function mockTxs(): Tx[] {
  const now = Date.now();
  return [
    { id: "mock_receive_001", amount: 420, address: "PRecvPepepowAddress999999", timestamp: now - 86_400_000, isSend: false },
    { id: "mock_send_001", amount: 69, address: "PSendPepepowAddress777777", timestamp: now - 172_800_000, isSend: true },
  ];
}

function Header({ title, onBack }: { title: string; onBack?: () => void }) {
  return (
    <div className="flex items-center gap-3 border-b border-green-100 px-4 py-3 text-green-800">
      {onBack && <button onClick={onBack} className="rounded-full p-1 active:scale-95"><ArrowLeft size={18} /></button>}
      <div className="font-mono text-sm font-bold tracking-widest">{title}</div>
    </div>
  );
}

function TxCard({ tx }: { tx: Tx }) {
  return (
    <div className="flex items-center justify-between rounded-2xl bg-white p-4 shadow-sm">
      <div className="flex items-center gap-3">
        <div className="rounded-xl bg-green-50 p-3 text-green-700">
          {tx.isSend ? <ArrowUpRight size={16} /> : <ArrowDownLeft size={16} />}
        </div>
        <div>
          <div className="text-sm font-bold text-slate-800">{tx.isSend ? "Sent PEPEW" : "Received PEPEW"}</div>
          <div className="font-mono text-[11px] text-slate-400">{shortText(tx.id, 8, 6)}</div>
          <div className="text-[11px] text-slate-400">{tx.isPending ? "Pending" : formatDate(tx.timestamp)}</div>
        </div>
      </div>
      <div className="text-right font-mono text-sm font-bold text-green-700">
        {tx.isSend ? "-" : "+"}{formatAmount(tx.amount, 2)}
      </div>
    </div>
  );
}

export default function App() {
  const [screen, setScreen] = useState<Screen>("seed");
  const [walletReady, setWalletReady] = useState(false);
  const [words, setWords] = useState(WORDS);
  const [checks, setChecks] = useState([false, false, false]);
  const [balance, setBalance] = useState(0);
  const [txs, setTxs] = useState<Tx[]>([]);
  const [apiState, setApiState] = useState<ApiState>("CONNECTED");
  const [apiMessage, setApiMessage] = useState("Read-only API mode. No signing or broadcast is enabled.");
  const [height, setHeight] = useState<string>("-");

  const allChecked = useMemo(() => checks.every(Boolean), [checks]);

  async function refreshApi() {
    setApiState("CONNECTED");
    setApiMessage("Loading read-only address summary from PEPEW Light API...");
    try {
      const [statusRes, summaryRes, historyRes] = await Promise.all([
        fetch(`${API_BASE}/api/status`),
        fetch(`${API_BASE}/api/wallet/address/${DEMO_ADDRESS}`),
        fetch(`${API_BASE}/api/wallet/history/${DEMO_ADDRESS}?limit=50&offset=0`),
      ]);

      const status = await statusRes.json().catch(() => ({}));
      const summary = await summaryRes.json().catch(() => ({}));
      const history = await historyRes.json().catch(() => ({}));

      const confirmed = summary?.balance?.confirmed_pepew ?? summary?.balance?.total_pepew ?? summary?.confirmed_pepew ?? summary?.balance ?? 0;
      const unconfirmed = summary?.balance?.unconfirmed_pepew ?? summary?.unconfirmed_pepew ?? 0;
      const parsedTxs = parseApiHistory(history?.history ?? history?.transactions ?? summary?.history ?? []);

      setBalance(safeNumber(confirmed) + safeNumber(unconfirmed));
      setTxs(parsedTxs);
      setHeight(safeText(status?.height ?? status?.block_height ?? "-"));
      setApiState(statusRes.ok && summaryRes.ok ? "READY" : "FAILED");
      setApiMessage(parsedTxs.length > 0 ? "Loaded read-only address summary from ElectrumX." : "API ready. No amount-bearing history entries returned for this address.");
    } catch (error) {
      setApiState("FAILED");
      setApiMessage(error instanceof Error ? error.message : "Unable to reach PEPEW Light API.");
      setTxs([]);
    }
  }

  useEffect(() => {
    if (walletReady) refreshApi();
  }, [walletReady]);

  const enterWallet = () => {
    setWalletReady(true);
    setScreen("dashboard");
  };

  if (screen === "seed") {
    return (
      <main className="min-h-screen bg-[#f1f8e9] text-slate-800">
        <Header title="🔑 GENERATE SEED PHRASE" />
        <section className="p-5">
          <p className="mb-4 text-sm text-slate-600">Write down these 12 words in order. This is mock-only prototype data.</p>
          <div className="grid grid-cols-3 gap-2 rounded-2xl border border-green-200 bg-green-50 p-4">
            {words.map((word, i) => (
              <div key={`${word}-${i}`} className="rounded-md border border-green-200 bg-white px-3 py-2 font-mono text-xs">
                <span className="text-slate-400">{i + 1}. </span>{word}
              </div>
            ))}
          </div>
          <button
            onClick={() => setWords([...words].sort(() => Math.random() - 0.5))}
            className="mt-4 w-full rounded-lg border border-green-200 bg-green-100 py-3 font-mono text-xs font-bold text-green-800"
          >
            SHUFFLE WORDS 🐸
          </button>
          <button
            onClick={() => setScreen("confirm")}
            className="mt-4 w-full rounded-lg bg-green-700 py-3 font-mono text-xs font-bold text-white"
          >
            NEXT
          </button>
        </section>
      </main>
    );
  }

  if (screen === "confirm") {
    return (
      <main className="min-h-screen bg-[#f1f8e9] text-slate-800">
        <Header title="CONFIRM BACKUP" onBack={() => setScreen("seed")} />
        <section className="space-y-3 p-5">
          {[
            "I understand this is a Phase 2 prototype.",
            "I understand no real mnemonic/private key is implemented.",
            "I understand sending is disabled until Phase 3.",
          ].map((label, i) => (
            <label key={label} className="flex items-center gap-3 rounded-2xl bg-white p-4 shadow-sm">
              <input
                type="checkbox"
                checked={checks[i]}
                onChange={() => setChecks(prev => prev.map((v, idx) => idx === i ? !v : v))}
              />
              <span className="text-sm text-slate-700">{label}</span>
            </label>
          ))}
          <button
            disabled={!allChecked}
            onClick={enterWallet}
            className="w-full rounded-lg bg-green-700 py-3 font-mono text-xs font-bold text-white disabled:bg-slate-300"
          >
            ENTER WALLET
          </button>
        </section>
      </main>
    );
  }

  if (screen === "receive") {
    return (
      <main className="min-h-screen bg-[#f1f8e9] text-slate-800">
        <Header title="Receive PEPEW" onBack={() => setScreen("dashboard")} />
        <section className="flex flex-col items-center gap-5 p-5 pt-20">
          <div className="rounded-3xl bg-white p-5 shadow-sm"><QrCode size={120} /></div>
          <div className="text-center text-xs font-bold uppercase tracking-widest text-slate-400">Your public address</div>
          <div className="break-all rounded-xl border border-green-200 bg-white p-4 text-center font-mono text-sm">{DEMO_ADDRESS}</div>
          <button className="fixed bottom-5 left-4 right-4 rounded-xl bg-green-700 py-4 font-mono text-xs font-bold text-white"><Copy className="mr-2 inline" size={14} />COPY ADDRESS</button>
        </section>
      </main>
    );
  }

  if (screen === "history") {
    return (
      <main className="min-h-screen bg-[#f1f8e9] text-slate-800">
        <Header title="Transaction History" onBack={() => setScreen("dashboard")} />
        <section className="space-y-3 p-4">
          {txs.length === 0 ? (
            <div className="rounded-2xl border border-green-200 bg-white p-6 text-center text-sm text-slate-500">No amount-bearing history entries available.</div>
          ) : txs.map(tx => <TxCard key={tx.id} tx={tx} />)}
        </section>
      </main>
    );
  }

  if (screen === "api") {
    return (
      <main className="min-h-screen bg-[#f1f8e9] text-slate-800">
        <Header title="PEPEW Light API Status" onBack={() => setScreen("dashboard")} />
        <section className="space-y-4 p-5">
          <div className="rounded-2xl bg-white p-5 shadow-sm">
            <div className="flex justify-between font-mono text-xs"><span>STATUS:</span><span className="text-green-700">{apiState}</span></div>
            <div className="mt-4 flex justify-between font-mono text-xs"><span>SERVER ENDPOINT:</span><span>{API_BASE.replace("https://", "")}</span></div>
            <div className="mt-4 flex justify-between font-mono text-xs"><span>BLOCK HEIGHT:</span><span className="text-green-700">{height}</span></div>
          </div>
          <div className="rounded-2xl border border-green-200 bg-green-50 p-4 text-xs text-green-800">{apiMessage}</div>
          <button onClick={refreshApi} className="rounded-xl bg-white px-4 py-3 font-mono text-xs font-bold text-green-700 shadow-sm"><RefreshCcw className="mr-2 inline" size={14} />REFRESH STATUS</button>
        </section>
      </main>
    );
  }

  if (screen === "settings") {
    return (
      <main className="min-h-screen bg-[#f1f8e9] text-slate-800">
        <Header title="Settings" onBack={() => setScreen("dashboard")} />
        <section className="space-y-4 p-5">
          <div className="rounded-2xl bg-white p-5 text-sm shadow-sm">
            <div className="flex justify-between"><span>App Name:</span><span>PEPEW Wallet</span></div>
            <div className="mt-3 flex justify-between"><span>Version:</span><span>1.0.0 Phase 2</span></div>
            <div className="mt-3 flex justify-between"><span>API Connection:</span><span>{API_BASE.replace("https://", "")}</span></div>
          </div>
          <div className="rounded-2xl border border-green-200 bg-green-50 p-4 text-xs text-green-800">This prototype is read-only. Real mnemonic, key derivation, signing, UTXO selection, and broadcast are absent.</div>
          <button onClick={() => { setWalletReady(false); setScreen("seed"); }} className="fixed bottom-5 left-4 right-4 rounded-xl bg-red-600 py-4 font-mono text-xs font-bold text-white">RESET WALLET DATA</button>
        </section>
      </main>
    );
  }

  if (screen === "send") {
    return (
      <main className="min-h-screen bg-[#f1f8e9] text-slate-800">
        <Header title="Send PEPEW" onBack={() => setScreen("dashboard")} />
        <section className="p-5">
          <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
            Send is disabled in Phase 2. Real signing and broadcast will be implemented later after local key handling is reviewed.
          </div>
        </section>
      </main>
    );
  }

  const recent = txs.length > 0 ? txs.slice(0, 3) : mockTxs();

  return (
    <main className="min-h-screen bg-[#f1f8e9] p-4 text-slate-800">
      <div className="mb-4 flex items-center justify-between py-2">
        <div className="font-mono text-sm font-bold text-green-800">🐸 PEPEW</div>
        <button onClick={() => setScreen("api")} className="rounded-full border border-green-200 bg-green-50 px-4 py-1 font-mono text-xs font-bold text-green-700">● {apiState}</button>
      </div>

      <section className="rounded-3xl bg-green-700 p-6 text-white shadow-sm">
        <div className="text-xs font-bold uppercase tracking-widest text-green-100">Balance</div>
        <div className="mt-3 font-mono text-2xl font-bold">{formatAmount(balance)} <span className="text-xs">PEPEW</span></div>
        <div className="mt-5 text-[11px] font-bold text-green-100">Demo Public Address: <span className="ml-2 font-mono underline">{shortText(DEMO_ADDRESS, 8, 6)}</span></div>
      </section>

      <section className="my-5 grid grid-cols-4 gap-3">
        {[
          ["Send", Send, "send"],
          ["Receive", ArrowDownLeft, "receive"],
          ["History", History, "history"],
          ["Settings", Settings, "settings"],
        ].map(([label, Icon, target]) => (
          <button key={String(label)} onClick={() => setScreen(target as Screen)} className="rounded-2xl bg-white p-4 text-center shadow-sm">
            {React.createElement(Icon as typeof Send, { size: 18, className: "mx-auto mb-3 text-green-700" })}
            <div className="text-xs font-bold text-slate-700">{String(label)}</div>
          </button>
        ))}
      </section>

      <section className="rounded-2xl bg-white p-4 shadow-sm">
        <div className="mb-3 flex items-center justify-between">
          <div className="font-mono text-xs font-bold uppercase tracking-widest text-slate-400">Recent Activity</div>
          <button onClick={refreshApi}><RefreshCcw size={14} className="text-slate-400" /></button>
        </div>
        <div className="space-y-3">
          {recent.map(tx => <TxCard key={tx.id} tx={tx} />)}
        </div>
      </section>
    </main>
  );
}
