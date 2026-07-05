import { ArrowDownLeft, ArrowUpRight } from "lucide-react";
import type { Tx } from "../types/wallet";
import { formatAmount, formatDate, shortText } from "../utils/format";

export function TxCard({ tx }: { tx: Tx }) {
  const title = tx.isUnknownAmount ? "Wallet Transaction" : tx.isSend ? "Sent PEPEW" : "Received PEPEW";
  const sign = tx.isUnknownAmount ? "" : tx.isSend ? "-" : "+";

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
