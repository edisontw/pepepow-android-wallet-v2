import type { RestoreValidationResult } from "./restoreTypes";

export function normalizeRestorePhrase(input: string): string[] {
  return input
    .trim()
    .toLowerCase()
    .split(/\s+/)
    .map(word => word.trim())
    .filter(Boolean);
}

export function validatePrototypeRestorePhrase(input: string): RestoreValidationResult {
  const words = normalizeRestorePhrase(input);

  if (words.length === 0) {
    return {
      ok: false,
      words,
      error: "Enter your 12-word test phrase.",
    };
  }

  if (words.length !== 12) {
    return {
      ok: false,
      words,
      error: `Expected exactly 12 words, found ${words.length}.`,
    };
  }

  return {
    ok: true,
    words,
  };
}
