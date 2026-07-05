import { addressToHash160, bytesToHex, getP2PKHScript } from "../wallet/walletUtils";
import type { UTXO } from "../wallet/walletUtils";
import type { Tx, WalletApiSnapshot } from "../types/wallet";
import { LIGHT_API_BASE } from "../config/appConstants";
import { safeNumber, safeText, normalizeTimestamp } from "../utils/format";

export function extractArray(payload: any): any[] {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.utxos)) return payload.utxos;
  if (Array.isArray(payload?.history)) return payload.history;
  if (Array.isArray(payload?.mempool)) return payload.mempool;
  if (Array.isArray(payload?.transactions)) return payload.transactions;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.items)) return payload.items;
  return [];
}

export function pepewFromApiAmount(value: unknown, explicitCoin = false): number {
  const n = safeNumber(value, 0);
  if (!Number.isFinite(n)) return 0;
  if (explicitCoin) return n;
  if (Number.isInteger(n) && Math.abs(n) >= 1_000_000) return n / 1e8;
  return n;
}

export function atomsFromApiValue(value: unknown, explicitAtoms = false): number {
  const n = safeNumber(value, 0);
  if (!Number.isFinite(n)) return 0;
  if (explicitAtoms) return Math.round(n);
  if (Number.isInteger(n) && Math.abs(n) >= 1_000_000) return Math.round(n);
  return Math.round(n * 1e8);
}

export function atomsFromUtxo(u: any): number {
  const explicitAtoms = u.satoshis ?? u.value_atoms ?? u.amount_atoms ?? u.atoms ?? u.value_sats;
  if (explicitAtoms !== undefined && explicitAtoms !== null && explicitAtoms !== "") {
    return atomsFromApiValue(explicitAtoms, true);
  }
  const value = u.value ?? u.amount ?? u.value_pepew ?? u.amount_pepew;
  return atomsFromApiValue(value, false);
}

export function scriptForAddress(address: string): string {
  return bytesToHex(getP2PKHScript(addressToHash160(address)));
}

export function parseUtxos(payload: any, fallbackAddress: string): UTXO[] {
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

export function parseApiHistory(items: unknown, address: string): Tx[] {
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

    return [{
      id,
      amount: Math.abs(delta),
      address: safeText(item.address ?? address),
      timestamp: normalizeTimestamp(timestamp),
      isSend,
      isPending: item.is_mempool === true || safeNumber(item.height, 1) <= 0 || item.pending === true,
      isUnknownAmount: !hasDelta,
    }];
  });
}

export async function fetchWalletSnapshot(address: string, options?: { fresh?: boolean; now?: number }): Promise<WalletApiSnapshot> {
  const now = options?.now ?? Date.now();
  const cacheBust = options?.fresh ? `?fresh=1&t=${now}` : "";
  const historyRefresh = options?.fresh ? `&fresh=1&t=${now}` : "";

  const [statusRes, summaryRes, historyRes, utxoRes] = await Promise.all([
    fetch(`${LIGHT_API_BASE}/api/status${cacheBust}`),
    fetch(`${LIGHT_API_BASE}/api/wallet/address/${address}${cacheBust}`),
    fetch(`${LIGHT_API_BASE}/api/wallet/history/${address}?limit=50&offset=0&detail_limit=10${historyRefresh}`),
    fetch(`${LIGHT_API_BASE}/api/wallet/utxo/${address}${cacheBust}`),
  ]);

  const status = await statusRes.json().catch(() => ({}));
  const summary = await summaryRes.json().catch(() => ({}));
  const history = await historyRes.json().catch(() => ({}));
  const utxoData = await utxoRes.json().catch(() => ({}));
  const historyItems = extractArray(history?.history ?? history?.transactions ?? summary?.history ?? []);
  const mempoolItems = extractArray(history?.mempool ?? []);
  const confirmedRaw = summary?.balance?.confirmed_pepew ?? summary?.balance?.total_pepew ?? summary?.confirmed_pepew ?? summary?.confirmed_balance ?? summary?.balance?.confirmed ?? summary?.balance;
  const unconfirmedRaw = summary?.balance?.unconfirmed_pepew ?? summary?.unconfirmed_pepew ?? summary?.mempool_balance ?? 0;
  const confirmed = pepewFromApiAmount(confirmedRaw, summary?.balance?.confirmed_pepew !== undefined || summary?.balance?.total_pepew !== undefined || summary?.confirmed_pepew !== undefined);
  const unconfirmed = pepewFromApiAmount(unconfirmedRaw, summary?.balance?.unconfirmed_pepew !== undefined || summary?.unconfirmed_pepew !== undefined);
  const parsedUtxos = parseUtxos(utxoData, address);
  const apiTxs = parseApiHistory([...mempoolItems, ...historyItems], address);
  const utxoBalance = parsedUtxos.reduce((sum, u) => sum + u.satoshis / 1e8, 0);

  return {
    balance: confirmed + unconfirmed || utxoBalance,
    txs: apiTxs,
    utxoCount: parsedUtxos.length,
    utxoTotal: utxoBalance,
    height: safeText(status?.height ?? status?.block_height ?? status?.data?.height ?? status?.electrumx?.height ?? status?.server?.height ?? status?.chain?.height ?? status?.tip?.height ?? "ready"),
    apiMessage: apiTxs.length > 0 ? `API ready. ${apiTxs.length} history entries, ${parsedUtxos.length} UTXOs.` : `API ready. ${parsedUtxos.length} UTXOs.`,
    apiState: statusRes.ok && summaryRes.ok ? "READY" : "FAILED",
  };
}
