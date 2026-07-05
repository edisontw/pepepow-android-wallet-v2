export function safeText(value: unknown): string {
  if (value === null || value === undefined) return "";
  return String(value);
}

export function safeNumber(value: unknown, fallback = 0): number {
  const parsed = typeof value === "number" ? value : Number(safeText(value).replace(/,/g, ""));
  return Number.isFinite(parsed) ? parsed : fallback;
}

export function shortText(value: unknown, head = 8, tail = 6): string {
  const text = safeText(value);
  if (text.length <= head + tail + 3) return text;
  return `${text.slice(0, head)}...${text.slice(-tail)}`;
}

export function formatAmount(value: unknown, digits = 4): string {
  return safeNumber(value).toLocaleString(undefined, {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  });
}

export function normalizeTimestamp(value: unknown): number {
  const n = safeNumber(value, Date.now());
  if (!Number.isFinite(n) || n <= 0) return Date.now();
  if (n > 1_000_000_000 && n < 10_000_000_000) return n * 1000;
  if (n > 1_000_000_000_000) return n;
  return Date.now();
}

export function formatDate(value: unknown): string {
  const date = new Date(normalizeTimestamp(value));
  if (Number.isNaN(date.getTime())) return "Unknown date";
  return date.toLocaleString(undefined, {
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}
