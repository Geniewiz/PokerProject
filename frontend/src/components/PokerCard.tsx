type PokerCardProps = {
  code: string;
  hidden?: boolean;
};

const SUIT_SYMBOL: Record<string, string> = {
  S: "♠",
  H: "♥",
  D: "♦",
  C: "♣"
};

function parseCard(code: string): { rank: string; suit: string; symbol: string; red: boolean } | null {
  const normalized = code.trim().toUpperCase();
  if (!normalized || normalized.length < 2) {
    return null;
  }

  const suit = normalized.slice(-1);
  const rawRank = normalized.slice(0, -1);
  const symbol = SUIT_SYMBOL[suit];
  if (!symbol) {
    return null;
  }

  const rank = rawRank === "T" ? "10" : rawRank;
  const red = suit === "H" || suit === "D";
  return { rank, suit, symbol, red };
}

export default function PokerCard({ code, hidden = false }: PokerCardProps) {
  if (hidden) {
    return <div className="poker-card back" aria-label="Hidden card" />;
  }

  const parsed = parseCard(code);
  if (!parsed) {
    return (
      <div className="poker-card unknown" title={code}>
        <span className="card-center">{code || "?"}</span>
      </div>
    );
  }

  return (
    <div className={`poker-card ${parsed.red ? "red" : "black"}`} title={code}>
      <div className="card-corner top">
        <span>{parsed.rank}</span>
        <span>{parsed.symbol}</span>
      </div>
      <div className="card-center">{parsed.symbol}</div>
      <div className="card-corner bottom">
        <span>{parsed.rank}</span>
        <span>{parsed.symbol}</span>
      </div>
    </div>
  );
}
