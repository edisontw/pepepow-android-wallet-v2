export type TransactionDisplayKind = "sent" | "received" | "self" | "unknown";

function text(value: unknown): string {
  return value === null || value === undefined ? "" : String(value);
}

export function classifyTransactionForAddress(item: Record<string, unknown>, ownAddress: string, delta: number): TransactionDisplayKind {
  const direction = text(item.direction).toLowerCase();
  const txType = text(item.type).toLowerCase();
  const toAddress = text(item.to_address ?? item.to ?? item.recipient ?? item.destination);
  const fromAddress = text(item.from_address ?? item.from ?? item.sender ?? item.source);
  const ownAddressCount = JSON.stringify(item).split(ownAddress).length - 1;
  const likelySent = direction.includes("sent") || direction.includes("send") || delta < 0;

  if (
    item.is_self_transfer === true ||
    direction.includes("self") ||
    txType.includes("self") ||
    (toAddress === ownAddress && fromAddress === ownAddress) ||
    (likelySent && ownAddressCount >= 2)
  ) {
    return "self";
  }

  if (likelySent) return "sent";
  if (delta > 0 || direction.includes("receive")) return "received";
  return "unknown";
}
