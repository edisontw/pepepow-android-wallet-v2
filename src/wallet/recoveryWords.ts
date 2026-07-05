const WORD_POOL = [
  "apple", "anchor", "bamboo", "beach", "bright", "candle", "circle", "cloud", "coffee", "coral", "dawn", "delta",
  "eagle", "earth", "ember", "forest", "garden", "gold", "green", "harbor", "island", "jungle", "kitten", "ladder",
  "lemon", "lotus", "maple", "market", "meadow", "moon", "morning", "mountain", "ocean", "olive", "orange", "paper",
  "pepper", "planet", "pond", "quiet", "river", "rocket", "silver", "spring", "stone", "summer", "sunset", "temple",
  "tiger", "valley", "violet", "wallet", "water", "window", "winter", "yellow", "zero", "zebra",
];

function randomIndex(max: number): number {
  const values = new Uint32Array(1);
  window.crypto.getRandomValues(values);
  return values[0] % max;
}

export function createRecoveryWords(): string[] {
  return Array.from({ length: 12 }, () => WORD_POOL[randomIndex(WORD_POOL.length)]);
}
