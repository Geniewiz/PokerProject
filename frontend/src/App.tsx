import { useMemo, useState } from "react";
import PhaserTable from "./components/PhaserTable";
import PokerCard from "./components/PokerCard";
import { createTable, getGame, sitAtTable, startGame } from "./lib/api";
import { HoldemWsClient } from "./lib/ws";
import type {
  ActionResultMessage,
  GameSnapshotResponse,
  PlayerActionResponse,
  TableSnapshotResponse
} from "./types/api";

export default function App() {
  const [userIdInput, setUserIdInput] = useState("1001");
  const [tableName, setTableName] = useState("alpha-table");
  const [botCount, setBotCount] = useState(1);
  const [seatNo, setSeatNo] = useState(1);
  const [raiseAmount, setRaiseAmount] = useState(200);
  const [status, setStatus] = useState("Idle");
  const [table, setTable] = useState<TableSnapshotResponse | null>(null);
  const [game, setGame] = useState<GameSnapshotResponse | null>(null);
  const [connected, setConnected] = useState(false);
  const ws = useMemo(() => new HoldemWsClient(), []);

  const userId = Number(userIdInput);

  const canPlay = Number.isFinite(userId) && !!table?.tableId;
  const mySeat = table?.seats.find((seat) => seat.player?.userId === userId) ?? null;
  const botPlayersCount =
    table?.seats.filter((seat) => (seat.player?.userId ?? 0) >= 900000000000).length ?? 0;
  const gameStarted = !!game?.handId;
  const gameFinished = !!game?.finished;
  const myTurn = !!game?.turn?.userId && game.turn.userId === userId;
  const userNameById = useMemo(() => {
    const map = new Map<number, string>();
    for (const seat of table?.seats ?? []) {
      if (seat.player) {
        map.set(seat.player.userId, seat.player.nickname);
      }
    }
    return map;
  }, [table]);
  const actionsByPhase = useMemo(() => {
    const phases = ["PREFLOP", "FLOP", "TURN", "RIVER", "SHOWDOWN"];
    const grouped = new Map<string, PlayerActionResponse[]>();
    for (const phase of phases) {
      grouped.set(phase, []);
    }
    for (const action of game?.recentActions ?? []) {
      if (!grouped.has(action.phase)) {
        grouped.set(action.phase, []);
      }
      grouped.get(action.phase)?.push(action);
    }
    return Array.from(grouped.entries()).filter(([, actions]) => actions.length > 0);
  }, [game]);
  const showdownEntries = useMemo(() => {
    const entries = game?.showdownResults ?? [];
    return [...entries].sort((a, b) => Number(b.winner) - Number(a.winner));
  }, [game]);

  async function handleCreateTable() {
    try {
      const created = await createTable(userId, tableName, 11);
      setTable(created);
      setStatus(`Table created: ${created.tableId}`);
    } catch (error) {
      setStatus(String(error));
    }
  }

  function handleConnectWs() {
    if (!table) {
      setStatus("Create table first");
      return;
    }
    ws.connect(table.tableId, {
      onConnect: () => setConnected(true),
      onDisconnect: () => setConnected(false),
      onTableSnapshot: (snapshot) => setTable(snapshot),
      onActionResult: (message: ActionResultMessage) => {
        const publicSnapshot = message.snapshot;
        if (publicSnapshot) {
          setGame((prev) => {
            if (!prev) {
              return publicSnapshot;
            }
            return {
              ...publicSnapshot,
              myHoleCards: prev.myHoleCards,
              availableActions: prev.availableActions
            };
          });
          getGame(userId, publicSnapshot.tableId)
            .then((privateSnapshot) => setGame(privateSnapshot))
            .catch(() => null);
        }
        setStatus(`Action event: ${message.action}`);
      }
    });
    setStatus("WebSocket connecting...");
  }

  function handleSit() {
    if (!table) {
      return;
    }
    sitAtTable(userId, table.tableId, seatNo, 1000)
      .then((snapshot) => {
        setTable(snapshot);
        setStatus(`Seated at #${seatNo}`);
      })
      .catch((error) => {
        setStatus(String(error));
      });
  }

  async function handleStart() {
    if (!table) {
      return;
    }
    if (!mySeat) {
      setStatus("먼저 Sit At Seat로 자리에 앉아주세요.");
      return;
    }
    try {
      const snapshot = await startGame(userId, table.tableId, botCount > 0, botCount);
      setGame(snapshot);
      setStatus("Game started");
    } catch (error) {
      setStatus(String(error));
    }
  }

  async function handleRefreshGame() {
    if (!table) {
      return;
    }
    try {
      const snapshot = await getGame(userId, table.tableId);
      setGame(snapshot);
      setStatus("Game refreshed");
    } catch (error) {
      setStatus(String(error));
    }
  }

  function sendAction(action: "FOLD" | "CHECK" | "CALL" | "RAISE") {
    if (!table) {
      return;
    }
    const amount =
      action === "CALL"
        ? game?.availableActions?.callAmount ?? 0
        : action === "RAISE"
          ? raiseAmount
          : 0;
    ws.sendAction(table.tableId, userId, action, amount);
    setStatus(`${action} sent`);
  }

  return (
    <div className="app">
      <aside className="panel">
        <h1>Texas Holdem Console</h1>
        <div className="badges">
          <span className={`badge ${connected ? "ok" : "off"}`}>
            WS {connected ? "Connected" : "Disconnected"}
          </span>
          <span className={`badge ${gameStarted ? "ok" : "off"}`}>
            Game {gameStarted ? (gameFinished ? "Finished" : "Started") : "Not Started"}
          </span>
          <span className={`badge ${myTurn ? "warn" : "off"}`}>
            {myTurn ? "Your Turn" : "Waiting"}
          </span>
        </div>

        <section className="card">
          <h2>Step 1. Identity</h2>
          <label>
            User ID
            <input value={userIdInput} onChange={(e) => setUserIdInput(e.target.value)} />
          </label>
        </section>

        <section className="card">
          <h2>Step 2. Table</h2>
          <label>
            Table name
            <input value={tableName} onChange={(e) => setTableName(e.target.value)} />
          </label>
          <div className="row">
            <button onClick={handleCreateTable}>Create Table</button>
            <button onClick={handleConnectWs} disabled={!table}>
              Connect WS
            </button>
          </div>
          <p className="meta">Table ID: {table?.tableId ?? "-"}</p>
        </section>

        <section className="card">
          <h2>Step 3. Sit</h2>
          <label>
            Seat no
            <input
              type="number"
              min={1}
              max={11}
              value={seatNo}
              onChange={(e) => setSeatNo(Number(e.target.value))}
            />
          </label>
          <div className="row">
            <button onClick={handleSit} disabled={!connected || !canPlay}>
              Sit At Seat
            </button>
            <button onClick={handleRefreshGame} disabled={!canPlay}>
              Refresh
            </button>
          </div>
          <p className="meta">My seat: {mySeat ? `#${mySeat.seatNo}` : "Not seated"}</p>
        </section>

        <section className="card">
          <h2>Step 4. Start Hand</h2>
          <label>
            Bot count (for start)
            <input
              type="number"
              min={0}
              max={10}
              value={botCount}
              onChange={(e) => setBotCount(Number(e.target.value))}
            />
          </label>
          <div className="row">
            <button className="primary" onClick={handleStart} disabled={!canPlay || !mySeat}>
              Start Hand
            </button>
          </div>
          <p className="meta">Bots at table: {botPlayersCount}</p>
        </section>

        <section className="card">
          <h2>Now</h2>
          <p className="meta">Status: {status}</p>
          <p className="meta">Hand: {game?.handId ?? "-"}</p>
          <p className="meta">Phase: {game?.phase ?? "-"}</p>
          <p className="meta">Turn user: {game?.turn?.userId ?? "-"}</p>
          <div className="card-group">
            <p className="meta">My cards</p>
            <div className="card-row">
              {(game?.myHoleCards?.length ? game.myHoleCards : ["", ""]).map((card, index) => (
                <PokerCard key={`my-${index}-${card}`} code={card} hidden={!card} />
              ))}
            </div>
          </div>
          <div className="card-group">
            <p className="meta">Board</p>
            <div className="card-row">
              {Array.from({ length: 5 }, (_, index) => game?.communityCards?.[index] ?? "").map(
                (card, index) => (
                  <PokerCard key={`board-${index}-${card}`} code={card} hidden={!card} />
                )
              )}
            </div>
          </div>
        </section>

        <section className="card">
          <h2>Action History</h2>
          {!actionsByPhase.length && <p className="meta">No actions yet.</p>}
          {!!actionsByPhase.length && (
            <div className="action-phase-list">
              {actionsByPhase.map(([phase, actions]) => (
                <div key={phase} className="phase-block">
                  <p className="phase-title">{phase}</p>
                  <div className="phase-actions">
                    {actions.map((action, index) => {
                      const actor = userNameById.get(action.userId) ?? `User ${action.userId}`;
                      const amountText = action.amount > 0 ? ` (${action.amount})` : "";
                      return (
                        <p className="action-line" key={`${phase}-${action.userId}-${index}`}>
                          {actor}: {action.action}
                          {amountText}
                        </p>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="card">
          <h2>Showdown</h2>
          {!game?.finished && <p className="meta">Hand in progress.</p>}
          {game?.finished && !showdownEntries.length && <p className="meta">No showdown details.</p>}
          {game?.finished && !!showdownEntries.length && (
            <div className="showdown-list">
              {showdownEntries.map((result) => (
                <div
                  className={`showdown-item ${result.winner ? "winner" : ""}`}
                  key={`showdown-${result.userId}`}
                >
                  <p className="showdown-title">
                    {(userNameById.get(result.userId) ?? `User ${result.userId}`) +
                      (result.winner ? " (Winner)" : "")}
                  </p>
                  <div className="card-row">
                    {result.holeCards.map((card, index) => (
                      <PokerCard key={`showdown-card-${result.userId}-${index}`} code={card} />
                    ))}
                  </div>
                  <p className="meta">Combo: {result.handRank}</p>
                </div>
              ))}
            </div>
          )}
        </section>

        <div className="action-bar">
          <h3>Action Controls</h3>
          <label>
            Raise amount
            <input
              type="number"
              min={1}
              value={raiseAmount}
              onChange={(e) => setRaiseAmount(Number(e.target.value))}
            />
          </label>
          <div className="row">
            <button onClick={() => sendAction("FOLD")} disabled={!canPlay}>
              Fold
            </button>
            <button onClick={() => sendAction("CHECK")} disabled={!canPlay}>
              Check
            </button>
            <button onClick={() => sendAction("CALL")} disabled={!canPlay}>
              Call
            </button>
            <button onClick={() => sendAction("RAISE")} disabled={!canPlay}>
              Raise
            </button>
          </div>
        </div>
      </aside>

      <main className="board">
        <PhaserTable table={table} game={game} />
        <section className="table-showdown">
          <h2>Table Showdown</h2>
          {!game?.finished && <p className="meta">핸드가 끝나면 승자와 조합 카드가 표시됩니다.</p>}
          {game?.finished && !showdownEntries.length && (
            <p className="meta">표시할 쇼다운 정보가 없습니다.</p>
          )}
          {game?.finished && !!showdownEntries.length && (
            <div className="table-showdown-list">
              {showdownEntries.map((result) => (
                <article
                  className={`table-showdown-item ${result.winner ? "winner" : ""}`}
                  key={`table-showdown-${result.userId}`}
                >
                  <p className="table-showdown-title">
                    {userNameById.get(result.userId) ?? `User ${result.userId}`}
                    {result.winner ? " - WINNER" : ""}
                  </p>
                  <p className="table-showdown-subtitle">Hole Cards</p>
                  <div className="card-row">
                    {result.holeCards.map((card, index) => (
                      <PokerCard key={`table-hole-${result.userId}-${index}`} code={card} />
                    ))}
                  </div>
                  <p className="table-showdown-subtitle">Combo Board</p>
                  <div className="card-row">
                    {Array.from({ length: 5 }, (_, index) => game.communityCards[index] ?? "").map(
                      (card, index) => (
                        <PokerCard
                          key={`table-board-${result.userId}-${index}-${card}`}
                          code={card}
                          hidden={!card}
                        />
                      )
                    )}
                  </div>
                  <p className="meta">Hand: {result.handRank}</p>
                </article>
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
