const RECENT_RECIPIENTS_KEY = "pepew_recent_recipients";
const MAX_RECENT_RECIPIENTS = 8;

export type RecentRecipient = {
  address: string;
  label?: string;
  lastUsedAt: number;
};

function safeParse(value: string | null): RecentRecipient[] {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    if (!Array.isArray(parsed)) return [];
    return parsed
      .map((item) => ({
        address: String(item?.address ?? "").trim(),
        label: item?.label ? String(item.label) : undefined,
        lastUsedAt: Number(item?.lastUsedAt ?? 0),
      }))
      .filter((item) => item.address.startsWith("P") && item.address.length >= 20);
  } catch {
    return [];
  }
}

export function loadRecentRecipients(): RecentRecipient[] {
  return safeParse(window.localStorage.getItem(RECENT_RECIPIENTS_KEY));
}

export function saveRecentRecipient(address: string, label?: string): RecentRecipient[] {
  const cleanAddress = address.trim();
  if (!cleanAddress.startsWith("P") || cleanAddress.length < 20) {
    return loadRecentRecipients();
  }

  const existing = loadRecentRecipients().filter((item) => item.address !== cleanAddress);
  const next = [
    { address: cleanAddress, label, lastUsedAt: Date.now() },
    ...existing,
  ].slice(0, MAX_RECENT_RECIPIENTS);

  window.localStorage.setItem(RECENT_RECIPIENTS_KEY, JSON.stringify(next));
  return next;
}

export function clearRecentRecipients() {
  window.localStorage.removeItem(RECENT_RECIPIENTS_KEY);
}
