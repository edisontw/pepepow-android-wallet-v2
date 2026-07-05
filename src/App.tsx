import React, { useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  ArrowDownLeft,
  ArrowLeft,
  ArrowUpRight,
  CheckCircle,
  Copy,
  Eye,
  EyeOff,
  History,
  QrCode,
  RefreshCcw,
  Send,
  Settings,
} from "lucide-react";
import {
  addressToHash160,
  bytesToHex,
  createAndSignTransaction,
  derivePrivateKeyFromMnemonic,
  getAddressFromPrivateKey,
  getP2PKHScript,
  privateKeyToWIF,
} from "./wallet/walletUtils";
import type { UTXO } from "./wallet/walletUtils";

type Screen = "seed" | "dashboard" | "receive" | "send" | "history" | "settings";
type ApiState = "CONNECTED" | "READY" | "FAILED";

type Tx = {
  id: string;
  amount: number;
  address: string;
  timestamp: number;
  isSend: boolean;
  isPending?: boolean;
};

type BroadcastResult = {
  success: boolean;
  txid?: string;
  error?: string;
};

const API_BASE = "https://light.pepepow.net";
const DEMO_ADDRESS = "PRfbEeHAKKbz6Voz85WJudrJwTA3ZbHunb";
const WORDS = ["swamp", "pepe", "key", "power", "wallet", "frog", "meme", "blockchain", "pond", "green", "crypto", "speed"];
const DEFAULT_RECIPIENT = "PL8s5WjXUGhHVSo743dwEXGtsifV5YpdcD";
const MOCK_FEE = 0.001;

function safeText(value: unknown): string {
  if (value === null || value === undefined) return "";
  return String(value);
}

function safeNumber(value: unknown, fallback = 0): number {
  const parsed = typeof value === "number" ? value : Number(safeText(value).replace(/,/g, ""));
  return Number.isFinite(parsed) ? parsed : fallback;
}

function shortText(value: unknown, head = 8, tail = 6): string {
  const text = safeText(value);
  if (text.length <= head + tail + 3) return text;
  return `${text.slice(0, head)}...${text.slice(-tail)}`;
}

function formatAmount(value: unknown, digits = 4): string {
  return safeNumber(value).toLocaleString(undefined, {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  });
}

function normalizeTimestamp(value: unknown): number {
  const n = safeNumber(value, Date.now());
  if (!Number.isFinite(n) || n <= 0) return Date.now();
  if (n > 1_000_000_000 && n < 10_000_000_000) return n * 1000;
  if (n > 1_000_000_000_000) return n;
  return Date.now();
}

function formatDate(value: unknown): string {
  const date = new Date(normalizeTimestamp(value));
  if (Number.isNaN(date.getTime())) return "Unknown date";
  return date.toLocaleString(undefined, { month: "short", day: "2-digit", hour: "2-digit", minute: "2-digit" });
}

function extractArray(payload: any): any[] {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.utxos)) return payload.utxos;
  if (Array.isArray(payload?.history)) return payload.history;
  if (Array.isArray(payload?.transactions)) return payload.transactions;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.items)) return payload.items;
  return [];
}

function pepewFromApiAmount(value: unknown, explicitCoin = false): number {
  const n = safeNumber(value, 0);
  if (!Number.isFinite(n)) return 0;
  if (explicitCoin) return n;
  if (Number.isInteger(n) && Math.abs(n) >= 1_000_000) return n / 1e8;
  return n;
}

function atomsFromApiValue(value: unknown, explicitAtoms = false): number {
  const n = safeNumber(value, 0);
  if (!Number.isFinite(n)) return 0;
  if (explicitAtoms) return Math.round(n);
  if (Number.isInteger(n) && Math.abs(n) >= 1_000_000) return Math.round(n);
  return Math.round(n * 1e8);
}

function atomsFromUtxo(u: any): number {
  const explicitAtoms = u.satoshis ?? u.value_atoms ?? u.amount_atoms ?? u.atoms ?? u.value_sats;
  if (explicitAtoms !== undefined && explicitAtoms !== null && explicitAtoms !== "") {
    return atomsFromApiValue(explicitAtoms, true);
  }
  const value = u.value ?? u.amount ?? u.value_pepew ?? u.amount_pepew;
  const explicitCoin = u.value_pepew !== undefined || u.amount_pepew !== undefined;
  return atomsFromApiValue(value, false && explicitCoin);
}

function scriptForAddress(address: string): string {
  return bytesToHex(getP2PKHScript(addressToHash160(address)));
}

function parseUtxos(payload: any, fallbackAddress: string): UTXO[] {
  const fallbackScript = scriptForAddress(fallbackAddress);
  return extractArray(payload)
    .map((u: any) => {
      const txid = safeText(u.txid ?? u.tx_hash ?? u.hash);
      const voutRaw = u.vout ?? u.tx_pos ?? u.index ?? u.n;
      const scriptPubKey = safeText(u.scriptPubKey ?? u.script_pub_key ?? u.script_pubkey ?? u.script ?? fallbackScript);
      return {
        txid,
        vout: safeNumber(voutRaw, -1),
        satoshis: atomsFromUtxo(u),
        scriptPubKey,
      };
    })
    .filter((u: UTXO) => u.txid.length === 64 && u.vout >= 0 && u.satoshis > 0 && u.scriptPubKey.length > 0);
}

function parseApiHistory(items: unknown, address: string, utxoPayload: any): Tx[] {
  if (!Array.isArray(items)) return [];
  const utxoByTxid = new Map<string, number>();
  for (const utxo of parseUtxos(utxoPayload, address)) {
    utxoByTxid.set(utxo.txid, (utxoByTxid.get(utxo.txid) ?? 0) + utxo.satoshis / 1e8);
  }

  return items.flatMap((raw, index) => {
    if (!raw || typeof raw !== "object") return [];
    const item = raw as Record<string, unknown>;
    const id = safeText(item.txid ?? item.tx_hash ?? item.hash ?? item.id ?? `tx_${index}`);
    const explicitCoin = item.amount_pepew !== undefined || item.value_pepew !== undefined || item.delta_pepew !== undefined;
    const amountRaw = item.amount_pepew ?? item.value_pepew ?? item.delta_pepew ?? item.amount_atoms ?? item.value_atoms ?? item.atoms ?? item.satoshis ?? item.amount ?? item.value ?? item.balance_delta;
    const apiAmount = amountRaw === undefined || amountRaw === null || amountRaw === "" ? 0 : pepewFromApiAmount(amountRaw, explicitCoin);
    const fallbackAmount = utxoByTxid.get(id) ?? 0;
    const signedAmount = apiAmount || fallbackAmount;
    const timestamp = item.timestamp ?? item.time ?? item.block_time ?? item.blockTime ?? Date.now();
    return [{
      id,
      amount: Math.abs(signedAmount),
      address: safeText(item.address ?? address),
      timestamp: normalizeTimestamp(timestamp),
      isSend: safeText(item.direction).toLowerCase().includes("send") || signedAmount < 0,
      isPending: safeNumber(item.height, 1) <= 0 || item.pending === true,
    }];
  });
}

function Header({ title, onBack }: { title: string; onBack?: () => void }) {
  return (
    <div className="sticky top-0 z-10 flex items-center gap-3 border-b border-green-100 bg-white px-4 py-3 text-green-800">
      {onBack && <button onClick={onBack} className="rounded-full p-1 hover:bg-green-50"><ArrowLeft size={18} /></button>}
      <div className="font-mono text-sm font-bold tracking-[0.25em]">{title}</div>
    </div>
  );
}

function TxCard({ tx }: { tx: Tx }) {
  return (
    <div className="flex items-center justify-between rounded-2xl border border-green-50 bg-white p-4 shadow-sm">
      <div className="flex items-center gap-3">
        <div className="rounded-xl bg-green-50 p-3 text-green-700">
          {tx.isSend ? <ArrowUpRight size={16} /> : <ArrowDownLeft size={16} />}
        </div>
        <div>
          <div className="text-sm font-bold text-slate-800">{tx.isSend ? "Sent PEPEW" : "Received PEPEW"}</div>
          <div className="break-all font-mono text-[11px] text-slate-400">{shortText(tx.id, 10, 8)}</div>
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
  const [inputSeedMode, setInputSeedMode] = useState(false);
  const [customSeedInput, setCustomSeedInput] = useState("");
  const [balance, setBalance] = useState(0);
  const [txs, setTxs] = useState<Tx[]>([]);
  const [apiState, setApiState] = useState<ApiState>("CONNECTED");
  const [apiMessage, setApiMessage] = useState("Experimental test wallet. Use small test funds only.");
  const [height, setHeight] = useState("-");
  const [recipient, setRecipient] = useState(DEFAULT_RECIPIENT);
  const [sendAmount, setSendAmount] = useState("10");
  const [sendError, setSendError] = useState("");
  const [signedTxHex, setSignedTxHex] = useState("");
  const [utxoCount, setUtxoCount] = useState(0);
  const [utxoTotal, setUtxoTotal] = useState(0);
  const [isBroadcasting, setIsBroadcasting] = useState(false);
  const [broadcastResult, setBroadcastResult] = useState<BroadcastResult | null>(null);
  const [showWif, setShowWif] = useState(false);
  const [useDemoAddress, setUseDemoAddress] = useState(false);
  const [copiedText, setCopiedText] = useState("");

  const localWallet = useMemo(() => {
    const mnemonic = words.join(" ").trim();
    const privKey = derivePrivateKeyFromMnemonic(mnemonic);
    return {
      privKey,
      address: getAddressFromPrivateKey(privKey, 55),
      wif: privateKeyToWIF(privKey, 204),
    };
  }, [words]);

  const activeAddress = useDemoAddress ? DEMO_ADDRESS : localWallet.address;

  async function refreshApi() {
    setApiState("CONNECTED");
    setApiMessage(`Querying ${shortText(activeAddress)}...`);
    try {
      const [statusRes, summaryRes, historyRes, utxoRes] = await Promise.all([
        fetch(`${API_BASE}/api/status`),
        fetch(`${API_BASE}/api/wallet/address/${activeAddress}`),
        fetch(`${API_BASE}/api/wallet/history/${activeAddress}?limit=50&offset=0`),
        fetch(`${API_BASE}/api/wallet/utxo/${activeAddress}`),
      ]);
      const status = await statusRes.json().catch(() => ({}));
      const summary = await summaryRes.json().catch(() => ({}));
      const history = await historyRes.json().catch(() => ({}));
      const utxoData = await utxoRes.json().catch(() => ({}));
      const confirmedRaw = summary?.balance?.confirmed_pepew ?? summary?.balance?.total_pepew ?? summary?.confirmed_pepew ?? summary?.confirmed_balance ?? summary?.balance?.confirmed ?? summary?.balance;
      const unconfirmedRaw = summary?.balance?.unconfirmed_pepew ?? summary?.unconfirmed_pepew ?? summary?.mempool_balance ?? 0;
      const confirmed = pepewFromApiAmount(confirmedRaw, summary?.balance?.confirmed_pepew !== undefined || summary?.balance?.total_pepew !== undefined || summary?.confirmed_pepew !== undefined);
      const unconfirmed = pepewFromApiAmount(unconfirmedRaw, summary?.balance?.unconfirmed_pepew !== undefined || summary?.unconfirmed_pepew !== undefined);
      const parsedUtxos = parseUtxos(utxoData, activeAddress);
      const parsedTxs = parseApiHistory(history?.history ?? history?.transactions ?? summary?.history ?? [], activeAddress, utxoData);
      setBalance(confirmed + unconfirmed || parsedUtxos.reduce((sum, u) => sum + u.satoshis / 1e8, 0));
      setTxs(parsedTxs);
      setUtxoCount(parsedUtxos.length);
      setUtxoTotal(parsedUtxos.reduce((sum, u) => sum + u.satoshis / 1e8, 0));
      setHeight(safeText(status?.height ?? status?.block_height ?? "-"));
      setApiState(statusRes.ok && summaryRes.ok ? "READY" : "FAILED");
      setApiMessage(parsedTxs.length > 0 ? `API ready. ${parsedTxs.length} history entries, ${parsedUtxos.length} UTXOs.` : `API ready. ${parsedUtxos.length} UTXOs.`);
    } catch (error) {
      setApiState("FAILED");
      setApiMessage(error instanceof Error ? error.message : "Unable to reach PEPEW Light API.");
      setTxs([]);
    }
  }

  useEffect(() => {
    if (walletReady) refreshApi();
  }, [walletReady, activeAddress]);

  function handleCopy(text: string, label: string) {
    navigator.clipboard?.writeText(text);
    setCopiedText(label);
    setTimeout(() => setCopiedText(""), 1800);
  }

  function importSeed() {
    const parts = customSeedInput.trim().split(/\s+/).filter(Boolean);
    if (parts.length !== 12) {
      setSendError("Seed phrase must be exactly 12 words.");
      return;
    }
    setWords(parts);
    setInputSeedMode(false);
    setCustomSeedInput("");
    setSendError("");
  }

  async function prepareLocalTransaction() {
    const cleanRecipient = recipient.trim();
    const amount = safeNumber(sendAmount, NaN);
    setSendError("");
    setSignedTxHex("");
    setBroadcastResult(null);
    setUtxoCount(0);
    setUtxoTotal(0);

    if (!cleanRecipient.startsWith("P") || cleanRecipient.length < 20) {
      setSendError("Enter a valid PEPEW recipient address starting with P.");
      return;
    }
    if (!Number.isFinite(amount) || amount <= 0) {
      setSendError("Enter a positive PEPEW amount.");
      return;
    }

    try {
      setSendError("Fetching live UTXOs from PEPEW Light API...");
      const utxoRes = await fetch(`${API_BASE}/api/wallet/utxo/${activeAddress}`);
      if (!utxoRes.ok) throw new Error(`Failed to query UTXOs. HTTP ${utxoRes.status}`);
      const utxoData = await utxoRes.json();
      const parsedUtxos = parseUtxos(utxoData, activeAddress);
      const total = parsedUtxos.reduce((sum, u) => sum + u.satoshis / 1e8, 0);
      setUtxoCount(parsedUtxos.length);
      setUtxoTotal(total);

      if (parsedUtxos.length === 0) {
        throw new Error(`The active address has 0 spendable UTXOs. Address: ${activeAddress}`);
      }

      setSendError(`Constructing and signing locally. UTXOs: ${parsedUtxos.length}, total: ${total.toFixed(8)} PEPEW.`);
      const rawHex = createAndSignTransaction(
        localWallet.privKey,
        parsedUtxos,
        cleanRecipient,
        amount,
        MOCK_FEE,
        activeAddress,
        55,
      );
      setSignedTxHex(rawHex);
      setSendError("");
    } catch (err) {
      setSendError(err instanceof Error ? err.message : "Error preparing transaction.");
      setSignedTxHex("");
    }
  }

  async function broadcastSignedTransaction() {
    if (!signedTxHex) return;
    setIsBroadcasting(true);
    setBroadcastResult(null);
    try {
      const res = await fetch(`${API_BASE}/api/wallet/broadcast`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ raw_tx: signedTxHex }),
      });
      const payload = await res.json().catch(() => ({}));
      if (!res.ok || payload?.error || payload?.detail) {
        throw new Error(safeText(payload?.error ?? payload?.message ?? payload?.detail ?? `Broadcast failed. HTTP ${res.status}`));
      }
      const txid = safeText(payload?.txid ?? payload?.tx_hash ?? payload?.result ?? payload?.data ?? "broadcasted");
      setBroadcastResult({ success: true, txid });
      setTxs(prev => [{ id: txid, amount: safeNumber(sendAmount), address: recipient.trim(), timestamp: Date.now(), isSend: true, isPending: true }, ...prev]);
      setSignedTxHex("");
      await refreshApi();
    } catch (err) {
      setBroadcastResult({ success: false, error: err instanceof Error ? err.message : "Broadcast failed." });
    } finally {
      setIsBroadcasting(false);
    }
  }

  if (screen === "seed") {
    return (
      <div className="min-h-screen bg-[#eef7e9] p-4 font-sans text-slate-900">
        <div className="mx-auto max-w-md space-y-4">
          <div className="rounded-3xl bg-white p-6 shadow-sm">
            <div className="mb-2 font-mono text-sm font-bold tracking-[0.25em] text-green-800">🐸 PEPEW WALLET</div>
            <h1 className="text-2xl font-black text-slate-900">Phase 3 Experimental</h1>
            <p className="mt-2 text-sm leading-6 text-slate-500">Browser-preview wallet for small test funds only. Seed/private key logic stays local, but this is not production wallet code.</p>
          </div>
          <div className="rounded-3xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
            <div className="mb-2 flex items-center gap-2 font-bold"><AlertTriangle size={16} /> Test wallet warning</div>
            <p>Do not use large balances. This prototype uses simplified deterministic derivation for AI Studio testing.</p>
          </div>
          <div className="rounded-3xl bg-white p-5 shadow-sm">
            <div className="mb-3 flex items-center justify-between">
              <div className="font-mono text-xs font-bold tracking-widest text-slate-400">SEED WORDS</div>
              <button onClick={() => setInputSeedMode(!inputSeedMode)} className="text-xs font-bold text-green-700">{inputSeedMode ? "Use Demo" : "Import Phrase"}</button>
            </div>
            {inputSeedMode ? (
              <div className="space-y-3">
                <textarea value={customSeedInput} onChange={e => setCustomSeedInput(e.target.value)} className="h-28 w-full rounded-2xl border border-green-100 p-3 font-mono text-xs outline-none focus:border-green-500" placeholder="Paste exactly 12 words" />
                <button onClick={importSeed} className="w-full rounded-xl bg-green-700 py-3 font-mono text-xs font-bold text-white">IMPORT LOCAL TEST WALLET</button>
              </div>
            ) : (
              <div className="grid grid-cols-3 gap-2">
                {words.map((word, index) => <div key={`${word}-${index}`} className="rounded-xl bg-green-50 p-2 font-mono text-xs"><span className="text-slate-400">{index + 1}.</span> {word}</div>)}
              </div>
            )}
          </div>
          {sendError && <div className="rounded-2xl bg-red-50 p-3 text-sm text-red-700">{sendError}</div>}
          <button onClick={() => { setWalletReady(true); setScreen("dashboard"); }} className="w-full rounded-2xl bg-green-700 py-4 font-mono text-sm font-bold tracking-widest text-white shadow-lg">CREATE / OPEN TEST WALLET</button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#eef7e9] pb-8 text-slate-900">
      {screen !== "dashboard" && <Header title={screen === "receive" ? "Receive PEPEW" : screen === "send" ? "Send PEPEW (secp256k1 sign)" : screen === "history" ? "Transaction History" : "Wallet Configuration"} onBack={() => setScreen("dashboard")} />}

      {screen === "dashboard" && (
        <main className="mx-auto max-w-md space-y-4 p-3">
          <div className="flex items-center justify-between py-2">
            <div className="font-mono text-xs font-black tracking-widest text-green-800">🐸 PEPEW WALLET <span className="rounded bg-green-50 px-2 py-1 text-[10px]">v1.0-Full</span></div>
            <div className={`rounded-full px-4 py-2 font-mono text-xs font-bold shadow-sm ${apiState === "READY" ? "bg-white text-green-700" : apiState === "FAILED" ? "bg-red-50 text-red-700" : "bg-white text-amber-600"}`}>● {apiState}</div>
          </div>
          <section className="overflow-hidden rounded-3xl bg-green-700 p-6 text-white shadow-lg">
            <div className="mb-4 flex items-center justify-between font-mono text-xs font-bold tracking-widest">LOCAL WALLET BALANCE <span className="rounded bg-green-600 px-2 py-1">P2PKH</span></div>
            <div className="font-mono text-3xl font-black tracking-widest">{formatAmount(balance)} <span className="text-sm">PEPEW</span></div>
            <div className="mt-5 border-t border-green-500 pt-4 font-mono text-xs">Address: <button onClick={() => handleCopy(activeAddress, "address")} className="underline">{shortText(activeAddress, 8, 6)}</button></div>
          </section>
          <div className="grid grid-cols-4 gap-3">
            <button onClick={() => setScreen("send")} className="rounded-2xl bg-white p-4 text-center shadow-sm"><Send className="mx-auto mb-2 text-green-700" size={18} /><div className="text-xs font-bold">Send</div></button>
            <button onClick={() => setScreen("receive")} className="rounded-2xl bg-white p-4 text-center shadow-sm"><ArrowDownLeft className="mx-auto mb-2 text-green-700" size={18} /><div className="text-xs font-bold">Receive</div></button>
            <button onClick={() => setScreen("history")} className="rounded-2xl bg-white p-4 text-center shadow-sm"><History className="mx-auto mb-2 text-green-700" size={18} /><div className="text-xs font-bold">History</div></button>
            <button onClick={() => setScreen("settings")} className="rounded-2xl bg-white p-4 text-center shadow-sm"><Settings className="mx-auto mb-2 text-green-700" size={18} /><div className="text-xs font-bold">Settings</div></button>
          </div>
          <section className="rounded-2xl bg-white p-4 shadow-sm">
            <div className="mb-3 flex items-center justify-between font-mono text-xs font-bold tracking-widest text-slate-400">RECENT ACTIVITY ({txs.length}) <button onClick={refreshApi}><RefreshCcw size={14} /></button></div>
            {txs.length === 0 ? <div className="py-8 text-center text-sm text-slate-400">No transaction history found on API.</div> : <div className="space-y-3">{txs.slice(0, 3).map(tx => <TxCard key={tx.id} tx={tx} />)}</div>}
          </section>
        </main>
      )}

      {screen === "receive" && (
        <main className="mx-auto max-w-md space-y-4 p-4">
          <section className="rounded-3xl bg-white p-5 shadow-sm">
            <div className="mb-3 font-mono text-xs font-bold tracking-widest text-slate-400">RECEIVE ADDRESS</div>
            <div className="break-all rounded-2xl bg-green-50 p-4 font-mono text-sm text-green-900">{activeAddress}</div>
            <button onClick={() => handleCopy(activeAddress, "address")} className="mt-3 flex w-full items-center justify-center gap-2 rounded-xl bg-green-700 py-3 font-mono text-xs font-bold text-white"><Copy size={14} /> COPY ADDRESS</button>
          </section>
          <section className="rounded-3xl bg-white p-5 text-center shadow-sm"><QrCode className="mx-auto text-green-700" size={120} /><div className="mt-2 text-xs text-slate-400">QR placeholder encodes address only in production UI.</div></section>
          <section className="rounded-3xl border border-red-100 bg-white p-5 shadow-sm">
            <button onClick={() => setShowWif(!showWif)} className="flex w-full items-center justify-between font-mono text-xs font-bold text-red-700">Reveal Private Key WIF {showWif ? <EyeOff size={16} /> : <Eye size={16} />}</button>
            {showWif && <div className="mt-3 break-all rounded-2xl bg-red-50 p-3 font-mono text-xs text-red-800">{localWallet.wif}</div>}
          </section>
        </main>
      )}

      {screen === "send" && (
        <main className="mx-auto max-w-md space-y-4 p-4">
          <section className="rounded-3xl bg-white p-5 shadow-sm">
            <label className="font-mono text-xs font-bold tracking-widest text-slate-500">RECIPIENT ADDRESS</label>
            <input value={recipient} onChange={e => setRecipient(e.target.value)} className="mt-2 w-full rounded-xl border border-green-100 p-3 font-mono text-sm outline-none focus:border-green-500" />
            <label className="mt-4 block font-mono text-xs font-bold tracking-widest text-slate-500">AMOUNT (PEPEW)</label>
            <input value={sendAmount} onChange={e => setSendAmount(e.target.value)} className="mt-2 w-full rounded-xl border border-green-100 p-3 font-mono text-sm outline-none focus:border-green-500" />
            <div className="mt-4 flex justify-between rounded-xl bg-slate-50 p-3 font-mono text-xs"><span>Standard local fee:</span><span className="font-bold text-green-700">{MOCK_FEE} PEPEW</span></div>
          </section>
          <div className="rounded-2xl bg-white p-3 font-mono text-[11px] text-slate-500">UTXO debug: {utxoCount} spendable · {formatAmount(utxoTotal, 8)} PEPEW</div>
          {sendError && <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-xs leading-5 text-amber-800">{sendError}</div>}
          {broadcastResult && <div className={`rounded-2xl p-4 text-xs ${broadcastResult.success ? "bg-green-50 text-green-800" : "bg-red-50 text-red-800"}`}>{broadcastResult.success ? `Broadcast submitted: ${broadcastResult.txid}` : broadcastResult.error}</div>}
          <button onClick={prepareLocalTransaction} className="w-full rounded-2xl bg-green-700 py-4 font-mono text-xs font-bold tracking-widest text-white">PREPARE & SIGN TRANSACTION (LOCAL)</button>
          <button onClick={broadcastSignedTransaction} disabled={!signedTxHex || isBroadcasting} className="w-full rounded-2xl bg-slate-900 py-4 font-mono text-xs font-bold tracking-widest text-white disabled:bg-slate-300">{isBroadcasting ? "BROADCASTING..." : "BROADCAST SIGNED TRANSACTION (LIVE)"}</button>
          {signedTxHex && <section className="rounded-3xl border border-green-100 bg-white p-5 shadow-sm"><div className="mb-2 flex items-center gap-2 font-mono text-xs font-bold text-green-700"><CheckCircle size={16} /> SIGNED RAW TX READY</div><div className="mb-2 text-xs text-slate-500">Inputs: {utxoCount} · Input total: {formatAmount(utxoTotal, 8)} PEPEW · To: {shortText(recipient, 10, 8)} · Amount: {sendAmount} PEPEW · Fee: {MOCK_FEE} PEPEW</div><pre className="max-h-48 overflow-auto rounded-2xl bg-slate-950 p-3 text-[10px] text-green-100">{signedTxHex}</pre></section>}
        </main>
      )}

      {screen === "history" && (
        <main className="mx-auto max-w-md space-y-4 p-4">
          <div className="rounded-2xl bg-white p-4 font-mono text-xs text-slate-500">Querying: <span className="font-bold text-green-800">{shortText(activeAddress, 10, 8)}</span></div>
          {txs.length === 0 ? <div className="rounded-3xl border border-green-200 bg-white p-8 text-center text-sm text-slate-500">No transactions returned from API. Receive coins or toggle the Demo Address in settings.</div> : txs.map(tx => <TxCard key={tx.id} tx={tx} />)}
        </main>
      )}

      {screen === "settings" && (
        <main className="mx-auto max-w-md space-y-4 p-4">
          <section className="rounded-3xl bg-white p-5 shadow-sm"><div className="flex justify-between border-b py-3"><span className="font-mono text-xs text-slate-400">App Name:</span><b>PEPEW Wallet</b></div><div className="flex justify-between border-b py-3"><span className="font-mono text-xs text-slate-400">Version:</span><b className="text-green-700">1.0.1 (Phase 3 Experimental)</b></div><div className="flex justify-between border-b py-3"><span className="font-mono text-xs text-slate-400">Network:</span><b>P2PKH v55</b></div><div className="flex justify-between py-3"><span className="font-mono text-xs text-slate-400">Height:</span><b>{height}</b></div></section>
          <section className="rounded-3xl bg-white p-5 shadow-sm"><div className="mb-3 font-mono text-xs font-bold tracking-widest text-slate-500">INTERACTIVE ADDRESS SWITCHING</div><label className="flex items-center justify-between rounded-2xl bg-green-50 p-4 text-sm font-bold text-green-900">Use Demo Read-Only Address<input type="checkbox" checked={useDemoAddress} onChange={e => setUseDemoAddress(e.target.checked)} /></label><div className="mt-3 break-all rounded-xl bg-slate-50 p-3 font-mono text-[11px] text-slate-400">Selected Address: {activeAddress}</div></section>
          <section className="rounded-3xl border border-green-200 bg-green-50 p-5 text-sm leading-6 text-green-900">🔒 Private keys, derived keys, and signing are local browser-preview logic. Never use this experimental prototype with large funds.</section>
          <button onClick={() => setScreen("seed")} className="w-full rounded-2xl bg-red-600 py-4 font-mono text-xs font-bold tracking-widest text-white">WIPE & RESET WALLET</button>
        </main>
      )}
      {copiedText && <div className="fixed bottom-4 left-1/2 -translate-x-1/2 rounded-full bg-slate-900 px-4 py-2 text-xs font-bold text-white shadow-lg">Copied {copiedText}</div>}
    </div>
  );
}
