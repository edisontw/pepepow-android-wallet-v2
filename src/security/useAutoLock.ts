import { useEffect } from "react";

export const DEFAULT_AUTO_LOCK_MS = 5 * 60 * 1000;

export function useAutoLock(options: {
  enabled: boolean;
  lastActiveAt: number;
  timeoutMs?: number;
  onLock: () => void;
}) {
  const { enabled, lastActiveAt, timeoutMs = DEFAULT_AUTO_LOCK_MS, onLock } = options;

  useEffect(() => {
    if (!enabled) return;

    const id = window.setInterval(() => {
      if (Date.now() - lastActiveAt >= timeoutMs) {
        onLock();
      }
    }, 10_000);

    return () => window.clearInterval(id);
  }, [enabled, lastActiveAt, timeoutMs, onLock]);

  useEffect(() => {
    if (!enabled) return;

    function handleVisibilityChange() {
      if (document.visibilityState === "hidden") {
        onLock();
      }
    }

    document.addEventListener("visibilitychange", handleVisibilityChange);
    return () => document.removeEventListener("visibilitychange", handleVisibilityChange);
  }, [enabled, onLock]);
}
