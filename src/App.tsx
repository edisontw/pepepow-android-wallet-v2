import React, { useEffect, useMemo, useState } from "react";
import {
  ArrowDownLeft,
  ArrowLeft,
  ArrowUpRight,
  CheckCircle,
  Copy,
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
import { SeedStartScreen } from "./screens/SeedStartScreen";
import { RestoreScreen } from "./screens/RestoreScreen";
import { RecentRecipientsCard } from "./screens/RecentRecipientsCard";
import { screenTitle } from "./screens/screenTitle";
import type { Screen } from "./types/wallet";
import { clearRecentRecipients, loadRecentRecipients, saveRecentRecipient } from "./wallet/recentRecipients";
import type { RecentRecipient } from "./wallet/recentRecipients";
import { createRecoveryWords } from "./wallet/recoveryWords";
import { SensitiveRevealCard } from "./screens/SensitiveRevealCard";
import { classifyTransactionForAddress } from "./wallet/transactionClassifier";

type ApiState = "CONNECTED" | "READY" | "FAILED";

type Tx = {
  id: string;
  amount: number;
  address: string;
  timestamp: number;
  isSend: boolean;
  isPending?: boolean;
  isUnknownAmount?: boolean;
  isSelfTransfer?: boolean;
};

type BroadcastResult = {
  success: boolean;
  txid?: string;
  error?: string;
};

type SendReview = {
  from: string;
  to: string;
  amount: number;
  fee: number;
  totalSpent: number;
  inputTotal: number;
  change: number;
  inputs: number;
};

const API_BASE = (import.meta as any).env?.DEV ? "" : "https://light.pepepow.net";
const DEFAULT_RECIPIENT = "";
const MOCK_FEE = 0.001;
const REFRESH_COOLDOWN_MS = 15_000;
const POST_BROADCAST_REFRESH_DELAYS_MS = [12_000, 45_000];

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
  if (Array.isArray(payload?.mempool)) return payload.mempool;
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
  return atomsFromApiValue(value, false);
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

function parseApiHistory(items: unknown, address: string): Tx[] {
  if (!Array.isArray(items)) return [];

  return items.flatMap((raw, index) => {
    if (!raw || typeof raw !== "object") return [];
    const item = raw as Record<string, unknown>;
    const id = safeText(item.txid ?? item.tx_hash ?? item.hash ?? item.id ?? `tx_${index}`);
    const deltaRaw = item.address_delta_pepew ?? item.delta_pepew ?? item.balance_delta_pepew ?? item.address_delta_atoms ?? item.delta_atoms ?? item.balance_delta_atoms ?? item.balance_delta;
    const hasDelta = deltaRaw !== undefined && deltaRaw !== null && deltaRaw !== "";
    const deltaIsCoin = item.address_delta_pepew !== undefined || item.delta_pepew !== undefined || item.balance_delta_pepew !== undefined;
    const delta = hasDelta ? pepewFromApiAmount(deltaRaw, deltaIsCoin) : 0;
    const direction = safeText(item.direction).toLowerCase();
    const isSend = direction.includes("sent") || direction.includes("send") || delta < 0;
    const timestamp = item.timestamp ?? item.time ?? item.block_time ?? item.blockTime ?? Date.now();
    
    const kind = classifyTransactionForAddress(item, address, delta);
    const isSelfTransfer = kind === "self";
    const classifiedIsSend = kind === "sent";

    return [{
      id,
      amount: Math.abs(delta),
      address: safeText(item.address ?? address),
      timestamp: normalizeTimestamp(timestamp),
      isSend: classifiedIsSend,
      isSelfTransfer,
      isPending: item.is_mempool === true || safeNumber(item.height, 1) <= 0 || item.pending === true,
      isUnknownAmount: !hasDelta,
    }];
  });
}

function mergeTxs(apiTxs: Tx[], localTxs: Tx[]): Tx[] {
  const seen = new Set<string>();
  return [...localTxs, ...apiTxs].filter(tx => {
    const key = tx.id || `${tx.address}-${tx.timestamp}-${tx.amount}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
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
  const title = tx.isUnknownAmount
    ? "Wallet Transaction"
    : tx.isSelfTransfer
      ? "Self Transfer"
      : tx.isSend
        ? "Sent PEPEW"
        : "Received PEPEW";
  const sign = tx.isUnknownAmount || tx.isSelfTransfer ? "" : tx.isSend ? "-" : "+";
  return (
    <div className="flex items-center justify-between rounded-2xl border border-green-50 bg-white p-4 shadow-sm">
      <div className="flex items-center gap-3">
        <div className="rounded-xl bg-green-50 p-3 text-green-700">
          {tx.isSend ? <ArrowUpRight size={16} /> : <ArrowDownLeft size={16} />}
        </div>
        <div>
          <div className="text-sm font-bold text-slate-800">{title}</div>
          <div className="break-all font-mono text-[11px] text-slate-400">{shortText(tx.id, 10, 8)}</div>
          <div className="text-[11px] text-slate-400">{tx.isPending ? "Pending / local" : formatDate(tx.timestamp)}</div>
        </div>
      </div>
      <div className="text-right font-mono text-sm font-bold text-green-700">
        {tx.isUnknownAmount ? "—" : `${sign}${formatAmount(tx.amount, 2)}`}
      </div>
    </div>
  );
}

export default function App() {
  const [screen, setScreen] = useState<Screen>("seed");
  const [walletReady, setWalletReady] = useState(false);
  const [words, setWords] = useState<string[]>(() => createRecoveryWords());
  const [balance, setBalance] = useState(0);
  const [txs, setTxs] = useState<Tx[]>([]);
  const [localTxs, setLocalTxs] = useState<Tx[]>([]);
  const [apiState, setApiState] = useState<ApiState>("CONNECTED");
  const [apiMessage, setApiMessage] = useState("PEPEW Light API is ready.");
  const [height, setHeight] = useState("-");
  const [lastRefreshLabel, setLastRefreshLabel] = useState("-");
  const [recipient, setRecipient] = useState(DEFAULT_RECIPIENT);
  const [sendAmount, setSendAmount] = useState("");
  const [sendError, setSendError] = useState("");
  const [signedTxHex, setSignedTxHex] = useState("");
  const [sendReview, setSendReview] = useState<SendReview | null>(null);
  const [submittedRawTxHex, setSubmittedRawTxHex] = useState("");
  const [utxoCount, setUtxoCount] = useState(0);
  const [utxoTotal, setUtxoTotal] = useState(0);
  const [isBroadcasting, setIsBroadcasting] = useState(false);
  const [broadcastResult, setBroadcastResult] = useState<BroadcastResult | null>(null);
  const [copiedText, setCopiedText] = useState("");
  const [lastRefreshAt, setLastRefreshAt] = useState(0);
  const [refreshCooldownSeconds, setRefreshCooldownSeconds] = useState(0);
  const [recentRecipients, setRecentRecipients] = useState<RecentRecipient[]>(() => loadRecentRecipients());

  const localWallet = useMemo(() => {
    const mnemonic = words.join(" ").trim();
    const privKey = derivePrivateKeyFromMnemonic(mnemonic);
    return {
      privKey,
      address: getAddressFromPrivateKey(privKey, 55),
      wif: privateKeyToWIF(privKey, 204),
    };
  }, [words]);

  const activeAddress = localWallet.address;
  const canBroadcast = !!signedTxHex && !isBroadcasting && submittedRawTxHex !== signedTxHex;

  useEffect(() => {
    const id = window.setInterval(() => {
      if (!lastRefreshAt) {
        setRefreshCooldownSeconds(0);
        return;
      }
      const remaining = Math.max(0, Math.ceil((REFRESH_COOLDOWN_MS - (Date.now() - lastRefreshAt)) / 1000));
      setRefreshCooldownSeconds(remaining);
    }, 1000);
    return () => window.clearInterval(id);
  }, [lastRefreshAt]);

  async function refreshApi(options?: { keepLocal?: boolean; force?: boolean; reason?: "initial" | "manual" | "auto" }) {
    const now = Date.now();
    const elapsed = now - lastRefreshAt;
    if (!options?.force && lastRefreshAt > 0 && elapsed < REFRESH_COOLDOWN_MS) {
      const waitSeconds = Math.ceil((REFRESH_COOLDOWN_MS - elapsed) / 1000);
      setApiMessage(`Refresh cooling down. Try again in ${waitSeconds}s.`);
      return;
    }

    setLastRefreshAt(now);
    setRefreshCooldownSeconds(Math.ceil(REFRESH_COOLDOWN_MS / 1000));
    setApiState("CONNECTED");
    setApiMessage(options?.reason === "manual" ? "Manual refresh started..." : `Querying ${shortText(activeAddress)}...`);
    try {
      const cacheBust = options?.force ? `?fresh=1&t=${now}` : "";
      const historyRefresh = options?.force ? `&fresh=1&t=${now}` : "";
      const [statusRes, summaryRes, historyRes, utxoRes] = await Promise.all([
        fetch(`${API_BASE}/api/status${cacheBust}`),
        fetch(`${API_BASE}/api/wallet/address/${activeAddress}${cacheBust}`),
        fetch(`${API_BASE}/api/wallet/history/${activeAddress}?limit=50&offset=0&detail_limit=10${historyRefresh}`),
        fetch(`${API_BASE}/api/wallet/utxo/${activeAddress}${cacheBust}`),
      ]);
      const status = await statusRes.json().catch(() => ({}));
      const summary = await summaryRes.json().catch(() => ({}));
      const history = await historyRes.json().catch(() => ({}));
      const utxoData = await utxoRes.json().catch(() => ({}));
      const historyItems = extractArray(history?.history ?? history?.transactions ?? summary?.history ?? []);
      const mempoolItems = extractArray(history?.mempool ?? []);
      const confirmedRaw = summary?.balance?.confirmed_pepew ?? summary?.balance?.total_pepew ?? summary?.confirmed_pepew ?? summary?.confirmed_balance ?? summary?.balance?.confirmed ?? summary?.[...];
      const unconfirmedRaw = summary?.balance?.unconfirmed_pepew ?? summary?.unconfirmed_pepew ?? summary?.mempool_balance ?? 0;
      const confirmed = pepewFromApiAmount(confirmedRaw, summary?.balance?.confirmed_pepew !== undefined || summary?.balance?.total_pepew !== undefined || summary?.confirmed_pepew !== undefined);
      const unconfirmed = pepewFromApiAmount(unconfirmedRaw, summary?.balance?.unconfirmed_pepew !== undefined || summary?.unconfirmed_pepew !== undefined);
      const parsedUtxos = parseUtxos(utxoData, activeAddress);
      const apiTxs = parseApiHistory([...mempoolItems, ...historyItems], activeAddress);
      const utxoBalance = parsedUtxos.reduce((sum, u) => sum + u.satoshis / 1e8, 0);
      setBalance(confirmed + unconfirmed || utxoBalance);
      setTxs(options?.keepLocal ? mergeTxs(apiTxs, localTxs) : apiTxs);
      setUtxoCount(parsedUtxos.length);
      setUtxoTotal(utxoBalance);
      setHeight(safeText(status?.height ?? status?.block_height ?? status?.data?.height ?? status?.electrumx?.height ?? status?.server?.height ?? status?.chain?.height ?? status?.tip?.height ?? "[...]"));
      setLastRefreshLabel(new Date(now).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit", second: "2-digit" }));
      setApiState(statusRes.ok && summaryRes.ok ? "READY" : "FAILED");
      setApiMessage(apiTxs.length > 0 ? `API ready. ${apiTxs.length} history entries, ${parsedUtxos.length} UTXOs.` : `API ready. ${parsedUtxos.length} UTXOs.`);
    } catch (error) {
      setApiState("FAILED");
      setApiMessage(error instanceof Error ? error.message : "Unable to reach PEPEW Light API.");
      setTxs(options?.keepLocal ? localTxs : []);
    }
  }

  useEffect(() => {
    if (walletReady) refreshApi({ keepLocal: true, reason: "initial", force: true });
  }, [walletReady, activeAddress]);

  function handleCopy(text: string, label: string) {
    navigator.clipboard?.writeText(text);
    setCopiedText(label);
    setTimeout(() => setCopiedText(""), 1800);
  }

  function clearPreparedTransaction() {
    setSignedTxHex("");
    setSendReview(null);
    setSendError("");
    setSubmittedRawTxHex("");
  }

  function openSend() {
    setBroadcastResult(null);
    clearPreparedTransaction();
    setScreen("send");
  }

  function resetWallet() {
    setWalletReady(false);
    setWords(createRecoveryWords());
    setBalance(0);
    setTxs([]);
    setLocalTxs([]);
    setRecipient("");
    setSendAmount("");
    setBroadcastResult(null);
    setLastRefreshAt(0);
    clearPreparedTransaction();
    setScreen("seed");
  }

  function restoreTestWallet(restoredWords: string[]) {
    setWords(restoredWords);
    setSendError("");
    setLocalTxs([]);
    setBroadcastResult(null);
    setLastRefreshAt(0);
    setWalletReady(true);
    setScreen("dashboard");
  }

  function selectRecentRecipient(address: string) {
    setRecipient(address);
    setBroadcastResult(null);
  }

  function clearRecipientHistory() {
    clearRecentRecipients();
    setRecentRecipients([]);
  }

  async function prepareLocalTransaction() {
    const cleanRecipient = recipient.trim();
    const amount = safeNumber(sendAmount, NaN);
    setSendError("");
    setSignedTxHex("");
    setSendReview(null);
    setSubmittedRawTxHex("");
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
      const utxoRes = await fetch(`${API_BASE}/api/wallet/utxo/${activeAddress}?fresh=1&t=${Date.now()}`);
      if (!utxoRes.ok) throw new Error(`Failed to query UTXOs. HTTP ${utxoRes.status}`);
      const utxoData = await utxoRes.json();
      const parsedUtxos = parseUtxos(utxoData, activeAddress);
      const inputTotal = parsedUtxos.reduce((sum, u) => sum + u.satoshis / 1e8, 0);
      const totalSpent = amount + MOCK_FEE;
      const change = inputTotal - totalSpent;
      setUtxoCount(parsedUtxos.length);
      setUtxoTotal(inputTotal);

      if (parsedUtxos.length === 0) {
        throw new Error(`The active address has 0 spendable UTXOs. Address: ${activeAddress}`);
      }
      if (change < 0) {
        throw new Error(`Insufficient spendable balance. Need ${totalSpent.toFixed(8)} PEPEW including fee.`);
      }

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
      setSendReview({
        from: activeAddress,
        to: cleanRecipient,
        amount,
        fee: MOCK_FEE,
        totalSpent,
        inputTotal,
        change,
        inputs: parsedUtxos.length,
      });
      setSendError("");
    } catch (err) {
      setSendError(err instanceof Error ? err.message : "Error preparing transaction.");
      setSignedTxHex("");
      setSendReview(null);
    }
  }

  async function broadcastSignedTransaction() {
    if (!canBroadcast) return;
    setIsBroadcasting(true);
    setBroadcastResult(null);
    const amount = sendReview?.amount ?? safeNumber(sendAmount);
    const fee = sendReview?.fee ?? MOCK_FEE;
    const isSelfTransfer = recipient.trim() === activeAddress;
    const optimisticTx: Tx = {
      id: `local-${Date.now()}`,
      amount,
      address: recipient.trim(),
      timestamp: Date.now(),
      isSend: !isSelfTransfer,
      isSelfTransfer,
      isPending: true,
    };
    try {
      const rawToSubmit = signedTxHex;
      setSubmittedRawTxHex(rawToSubmit);
      const res = await fetch(`${API_BASE}/api/wallet/broadcast`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ raw_tx: rawToSubmit }),
      });
      const payload = await res.json().catch(() => ({}));
      if (!res.ok || payload?.error || payload?.detail) {
        setSubmittedRawTxHex("");
        throw new Error(safeText(payload?.error?.message ?? payload?.error ?? payload?.message ?? payload?.detail ?? `Broadcast failed. HTTP ${res.status}`));
      }
      const txid = safeText(payload?.txid ?? payload?.tx_hash ?? payload?.result ?? payload?.data ?? optimisticTx.id);
      const submittedTx = { ...optimisticTx, id: txid };
      const nextLocal = [submittedTx, ...localTxs];
      setLocalTxs(nextLocal);
      setTxs(prev => mergeTxs(prev, [submittedTx]));
      const balanceDeduction = isSelfTransfer ? fee : amount + fee;
      setBalance(prev => Math.max(0, prev - balanceDeduction));
      setUtxoTotal(prev => Math.max(0, prev - balanceDeduction));
      setUtxoCount(0);
      setRecentRecipients(saveRecentRecipient(recipient.trim()));
      setBroadcastResult({ success: true, txid });
      setSignedTxHex("");
      setSendReview(null);
      setSendError("Broadcast submitted. Auto refresh is rate-limited to reduce server load; use manual refresh after cooldown if needed.");
      POST_BROADCAST_REFRESH_DELAYS_MS.forEach(delay => {
        window.setTimeout(() => refreshApi({ keepLocal: true, force: true, reason: "auto" }), delay);
      });
    } catch (err) {
      setBroadcastResult({ success: false, error: err instanceof Error ? err.message : "Broadcast failed." });
    } finally {
      setIsBroadcasting(false);
    }
  }

  if (screen === "seed") {
    return (
      <SeedStartScreen
        words={words}
        error={sendError}
        onCreate={() => {
          setWords(createRecoveryWords());
          setWalletReady(false);
          setScreen("backup");
        }}
        onRestore={() => setScreen("restore")}
      />
    );
  }

  if (screen === "restore") {
    return (
      <div className="min-h-screen bg-[#eef7e9] pb-8 text-slate-900">
        <RestoreScreen
          onRestore={restoreTestWallet}
          onCancel={() => setScreen("seed")}
        />
      </div>
    );
  }

  if (screen === "backup") {
    return (
      <div className="min-h-screen bg-[#eef7e9] pb-8 text-slate-900">
        <Header title="BACKUP RECOVERY WORDS" onBack={() => setScreen("seed")} />
        <main className="mx-auto max-w-md space-y-4 p-4">
          <section className="rounded-3xl border border-amber-200 bg-amber-50 p-5 shadow-sm">
            <div className="mb-3 font-mono text-xs font-bold text-amber-800">⚠️ IMPORTANT</div>
            <div className="text-sm leading-6 text-amber-900">
              Write these 12 words down in order. Anyone with these words can spend your funds.
            </div>
          </section>
          <section className="rounded-3xl bg-white p-5 shadow-sm">
            <div className="mb-4 font-mono text-xs font-bold tracking-widest text-slate-400">YOUR RECOVERY WORDS</div>
            <div className="grid grid-cols-2 gap-3">
              {words.map((word, index) => (
                <div key={index} className="flex items-center gap-2 rounded-xl bg-green-50 p-3">
                  <div className="font-mono text-xs font-bold text-slate-400 w-6">{index + 1}.</div>
                  <div className="font-mono text-sm font-bold text-green-900">{word}</div>
                </div>
              ))}
            </div>
          </section>
          <button
            onClick={() => {
              setWalletReady(true);
              setScreen("dashboard");
            }}
            className="w-full rounded-2xl bg-green-700 py-4 font-mono text-xs font-bold tracking-widest text-white"
          >
            I HAVE BACKED UP THESE WORDS
          </button>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#eef7e9] pb-8 text-slate-900">
      {screen !== "dashboard" && <Header title={screenTitle(screen)} onBack={() => setScreen("dashboard")} />}

      {screen === "dashboard" && (
        <main className="mx-auto max-w-md space-y-4 p-3">
          <div className="flex items-center justify-between py-2">
            <div className="font-mono text-xs font-black tracking-widest text-green-800">PEPEW WALLET <span className="rounded bg-green-50 px-2 py-1 text-[10px]">v1.0</span></div>
            <div className={`rounded-full px-4 py-2 font-mono text-xs font-bold shadow-sm ${apiState === "READY" ? "bg-white text-green-700" : apiState === "FAILED" ? "bg-red-50 text-red-700" : "bg-yellow-50 text-yellow-700"}`}>{apiState}</div>
          </div>
          <section className="overflow-hidden rounded-3xl bg-green-700 p-6 text-white shadow-lg">
            <div className="mb-4 flex items-center justify-between font-mono text-xs font-bold tracking-widest">LOCAL WALLET BALANCE <span className="rounded bg-green-600 px-2 py-1">P2PKH</span></div>
            <div className="font-mono text-3xl font-black tracking-widest">{formatAmount(balance)} <span className="text-sm">PEPEW</span></div>
            <div className="mt-5 border-t border-green-500 pt-4 font-mono text-xs">Address: <button onClick={() => handleCopy(activeAddress, "address")} className="underline">{shortText(activeAddress)}</button></div>
          </section>
          <div className="rounded-2xl bg-white p-3 font-mono text-[11px] text-slate-500">API refresh cooldown: {refreshCooldownSeconds > 0 ? `${refreshCooldownSeconds}s` : "ready"}</div>
          <div className="grid grid-cols-4 gap-3">
            <button onClick={openSend} className="rounded-2xl bg-white p-4 text-center shadow-sm"><Send className="mx-auto mb-2 text-green-700" size={18} /><div className="text-xs font-bold">Send</div></button>
            <button onClick={() => setScreen("receive")} className="rounded-2xl bg-white p-4 text-center shadow-sm"><ArrowDownLeft className="mx-auto mb-2 text-green-700" size={18} /><div className="text-xs font-bold">Receive</div></button>
            <button onClick={() => setScreen("history")} className="rounded-2xl bg-white p-4 text-center shadow-sm"><History className="mx-auto mb-2 text-green-700" size={18} /><div className="text-xs font-bold">History</div></button>
            <button onClick={() => setScreen("settings")} className="rounded-2xl bg-white p-4 text-center shadow-sm"><Settings className="mx-auto mb-2 text-green-700" size={18} /><div className="text-xs font-bold">Settings</div></button>
          </div>
          <section className="rounded-2xl bg-white p-4 shadow-sm">
            <div className="mb-3 flex items-center justify-between font-mono text-xs font-bold tracking-widest text-slate-400">RECENT ACTIVITY ({txs.length}) <button disabled={refreshCooldownSeconds > 0} onClick={() => refreshApi({ keepLocal: true, reason: "manual" })} className="text-green-700 hover:text-green-900 disabled:text-slate-300"><RefreshCcw size={14} /></button></div>
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
          <section className="rounded-3xl bg-white p-5 text-center shadow-sm"><QrCode className="mx-auto text-green-700" size={120} /><div className="mt-2 text-xs text-slate-400">QR code</div></section>
          <section className="rounded-3xl border border-red-100 bg-white p-5 shadow-sm">
            <SensitiveRevealCard
              title="Private Key WIF"
              description="Anyone with this private key can spend funds from this wallet. Reveal it only in a private environment."
              value={localWallet.wif}
              revealLabel="REVEAL PRIVATE KEY"
            />
          </section>
        </main>
      )}

      {screen === "send" && (
        <main className="mx-auto max-w-md space-y-4 p-4">
          <RecentRecipientsCard recipients={recentRecipients} onSelect={selectRecentRecipient} onClear={clearRecipientHistory} />
          <section className="rounded-3xl bg-white p-5 shadow-sm">
            <label className="font-mono text-xs font-bold tracking-widest text-slate-500">RECIPIENT ADDRESS</label>
            <input value={recipient} disabled={!!signedTxHex || isBroadcasting} onChange={e => { setRecipient(e.target.value); setBroadcastResult(null); }} className="mt-2 w-full rounded-xl border border-green-100 px-3 py-2 font-mono text-sm outline-none focus:ring-2 focus:ring-green-300" />
            <label className="mt-4 block font-mono text-xs font-bold tracking-widest text-slate-500">AMOUNT (PEPEW)</label>
            <input value={sendAmount} disabled={!!signedTxHex || isBroadcasting} onChange={e => { setSendAmount(e.target.value); setBroadcastResult(null); }} className="mt-2 w-full rounded-xl border border-green-100 px-3 py-2 font-mono text-sm outline-none focus:ring-2 focus:ring-green-300" />
            <div className="mt-4 flex justify-between rounded-xl bg-slate-50 p-3 font-mono text-xs"><span>Network fee:</span><span className="font-bold text-green-700">{MOCK_FEE} PEPEW</span></div>
          </section>
          <div className="rounded-2xl bg-white p-3 font-mono text-[11px] text-slate-500">UTXO: {utxoCount} spendable · {formatAmount(utxoTotal, 8)} PEPEW</div>
          {sendError && <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-xs leading-5 text-amber-800">{sendError}</div>}
          {broadcastResult && <div className={`rounded-2xl p-4 text-xs ${broadcastResult.success ? "bg-green-50 text-green-800" : "bg-red-50 text-red-800"}`}>{broadcastResult.success ? `Broadcast successful. TXID: ${shortText(broadcastResult.txid, 10, 8)}` : `Error: ${broadcastResult.error}`}</div>}
          {!signedTxHex && <button onClick={prepareLocalTransaction} className="w-full rounded-2xl bg-green-700 py-4 font-mono text-xs font-bold tracking-widest text-white">PREPARE TRANSACTION</button>}
          {sendReview && (
            <section className="rounded-3xl border border-green-100 bg-white p-5 shadow-sm">
              <div className="mb-3 flex items-center gap-2 font-mono text-xs font-bold text-green-700"><CheckCircle size={16} /> REVIEW BEFORE BROADCAST</div>
              <div className="space-y-2 font-mono text-[11px] text-slate-600">
                <div className="flex justify-between gap-3"><span>From</span><span className="text-right text-slate-900">{shortText(sendReview.from, 10, 8)}</span></div>
                <div className="flex justify-between gap-3"><span>To</span><span className="text-right text-slate-900">{shortText(sendReview.to, 10, 8)}</span></div>
                <div className="flex justify-between"><span>Amount</span><b>{formatAmount(sendReview.amount, 8)} PEPEW</b></div>
                <div className="flex justify-between"><span>Fee</span><b>{formatAmount(sendReview.fee, 8)} PEPEW</b></div>
                <div className="flex justify-between"><span>Total spent</span><b>{formatAmount(sendReview.totalSpent, 8)} PEPEW</b></div>
                <div className="flex justify-between"><span>Input total</span><b>{formatAmount(sendReview.inputTotal, 8)} PEPEW</b></div>
                <div className="flex justify-between"><span>Change</span><b>{formatAmount(sendReview.change, 8)} PEPEW</b></div>
              </div>
            </section>
          )}
          {signedTxHex && <button onClick={clearPreparedTransaction} className="w-full rounded-2xl bg-white py-3 font-mono text-xs font-bold tracking-widest text-green-700 shadow-sm">EDIT DETAILS</button>}
          <button onClick={broadcastSignedTransaction} disabled={!canBroadcast} className="w-full rounded-2xl bg-slate-900 py-4 font-mono text-xs font-bold tracking-widest text-white disabled:bg-slate-300">BROADCAST TRANSACTION</button>
          {signedTxHex && <section className="rounded-3xl border border-green-100 bg-white p-5 shadow-sm"><div className="mb-2 font-mono text-xs font-bold text-green-700">SIGNED RAW TX READY</div><div className="break-all rounded-2xl bg-green-50 p-3 font-mono text-[9px] text-green-900">{signedTxHex}</div></section>}
        </main>
      )}

      {screen === "history" && (
        <main className="mx-auto max-w-md space-y-4 p-4">
          <div className="rounded-2xl bg-white p-4 font-mono text-xs text-slate-500">Querying: <span className="font-bold text-green-800">{shortText(activeAddress, 10, 8)}</span></div>
          <button disabled={refreshCooldownSeconds > 0} onClick={() => refreshApi({ keepLocal: true, reason: "manual" })} className="w-full rounded-2xl bg-white py-3 font-mono text-xs font-bold tracking-widest text-green-700 shadow-sm disabled:text-slate-300">MANUAL REFRESH{refreshCooldownSeconds > 0 && ` IN ${refreshCooldownSeconds}s`}</button>
          {txs.length === 0 ? <div className="rounded-3xl border border-green-200 bg-white p-8 text-center text-sm text-slate-500">No transactions returned from API. Recent local broadcasts stay in the history above.</div> : <div className="space-y-3">{txs.map(tx => <TxCard key={tx.id} tx={tx} />)}</div>}
        </main>
      )}

      {screen === "settings" && (
        <main className="mx-auto max-w-md space-y-4 p-4">
          <section className="rounded-3xl bg-white p-5 shadow-sm"><div className="flex justify-between border-b py-3"><span className="font-mono text-xs text-slate-400">App Name:</span><b>PEPEW Wallet</b></div><div className="flex justify-between border-b py-3 font-mono text-xs"><span className="text-slate-400">Version:</span><span className="text-slate-900">1.0</span></div></section>
          <section className="rounded-3xl bg-white p-5 shadow-sm">
            <SensitiveRevealCard
              title="Recovery Words"
              description="Anyone with these words can restore and spend this wallet. Reveal them only in a private environment."
              value={words.join(" ")}
              revealLabel="REVEAL RECOVERY WORDS"
            />
          </section>
          <section className="rounded-3xl border border-green-200 bg-green-50 p-5 text-sm leading-6 text-green-900">🔒 Private keys, derived keys, and signing are local. Recovery words and private keys are generated and stored only in browser memory.</section>
          <button onClick={resetWallet} className="w-full rounded-2xl bg-red-600 py-4 font-mono text-xs font-bold tracking-widest text-white">WIPE & RESET WALLET</button>
        </main>
      )}
      {copiedText && <div className="fixed bottom-4 left-1/2 -translate-x-1/2 rounded-full bg-slate-900 px-4 py-2 text-xs font-bold text-white shadow-lg">Copied {copiedText}</div>}
    </div>
  );
}
