import { Clock, Trash2 } from "lucide-react";
import type { RecentRecipient } from "../wallet/recentRecipients";

type RecentRecipientsCardProps = {
  recipients: RecentRecipient[];
  onSelect: (address: string) => void;
  onClear: () => void;
};

function shortAddress(address: string) {
  if (address.length <= 18) return address;
  return `${address.slice(0, 10)}...${address.slice(-8)}`;
}

export function RecentRecipientsCard({ recipients, onSelect, onClear }: RecentRecipientsCardProps) {
  if (recipients.length === 0) return null;

  return (
    <section className="rounded-3xl bg-white p-5 shadow-sm">
      <div className="mb-3 flex items-center justify-between">
        <div className="flex items-center gap-2 font-mono text-xs font-bold tracking-widest text-slate-500">
          <Clock size={14} /> RECENT RECIPIENTS
        </div>
        <button onClick={onClear} className="rounded-full p-1 text-slate-400 hover:bg-slate-50 hover:text-red-600">
          <Trash2 size={14} />
        </button>
      </div>
      <div className="space-y-2">
        {recipients.map((recipient) => (
          <button
            key={recipient.address}
            onClick={() => onSelect(recipient.address)}
            className="w-full rounded-2xl bg-green-50 px-3 py-2 text-left font-mono text-xs text-green-900 hover:bg-green-100"
          >
            {recipient.label && <div className="mb-1 font-bold">{recipient.label}</div>}
            <div>{shortAddress(recipient.address)}</div>
          </button>
        ))}
      </div>
    </section>
  );
}
