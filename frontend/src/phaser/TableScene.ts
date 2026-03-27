import Phaser from "phaser";
import type { GameSnapshotResponse, TableSnapshotResponse } from "../types/api";

type RenderState = {
  table: TableSnapshotResponse | null;
  game: GameSnapshotResponse | null;
};

function actionSummary(action: string, amount: number): string {
  return amount > 0 ? `${action} ${amount}` : action;
}

function parseCard(code: string): { rank: string; suit: string; red: boolean } | null {
  const normalized = code.trim().toUpperCase();
  if (!normalized || normalized.length < 2) {
    return null;
  }
  const suit = normalized.slice(-1);
  const rankToken = normalized.slice(0, -1);
  if (!["S", "H", "D", "C"].includes(suit)) {
    return null;
  }
  const rank = rankToken === "T" ? "10" : rankToken;
  return { rank, suit, red: suit === "H" || suit === "D" };
}

function suitSymbol(suit: string): string {
  if (suit === "S") {
    return "♠";
  }
  if (suit === "H") {
    return "♥";
  }
  if (suit === "D") {
    return "♦";
  }
  if (suit === "C") {
    return "♣";
  }
  return "?";
}

export class TableScene extends Phaser.Scene {
  private state: RenderState = { table: null, game: null };
  private ready = false;
  private pendingState: RenderState | null = null;
  private previousGame: GameSnapshotResponse | null = null;
  private seatPointsByUserId = new Map<number, Phaser.Math.Vector2>();
  private potPoint = new Phaser.Math.Vector2(0, 0);
  private deckPoint = new Phaser.Math.Vector2(0, 0);
  private boardPoints: Phaser.Math.Vector2[] = [];

  constructor() {
    super("TableScene");
  }

  create(): void {
    this.ready = true;
    if (this.pendingState) {
      this.state = this.pendingState;
      this.pendingState = null;
    }
    this.render();
  }

  updateState(next: RenderState): void {
    if (!this.ready) {
      this.pendingState = next;
      return;
    }
    const prevGame = this.state.game;
    this.state = next;
    this.render();
    this.animateDiff(prevGame, next.game);
    this.previousGame = next.game;
  }

  private render(): void {
    this.children.removeAll();
    this.seatPointsByUserId.clear();
    this.boardPoints = [];
    const width = this.scale.width;
    const height = this.scale.height;

    this.add.rectangle(width / 2, height / 2, width - 30, height - 30, 0x0d3325, 0.95);
    this.add.ellipse(width / 2, height / 2, width - 120, height - 170, 0x14553d, 1);
    this.add.text(24, 18, "Holdem Table", {
      fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace",
      fontSize: "20px",
      color: "#def7ea"
    });

    if (!this.state.table) {
      this.add.text(width / 2 - 120, height / 2 - 10, "Create table and connect WS", {
        fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace",
        fontSize: "16px",
        color: "#d1fae5"
      });
      return;
    }

    const seats = this.state.table.seats;
    const radiusX = (width - 200) / 2;
    const radiusY = (height - 230) / 2;
    const cx = width / 2;
    const cy = height / 2 + 8;
    this.potPoint = new Phaser.Math.Vector2(width / 2, height / 2 + 12);
    this.deckPoint = new Phaser.Math.Vector2(width / 2 + 250, height / 2 - 20);

    this.add.rectangle(this.deckPoint.x, this.deckPoint.y, 56, 74, 0x0f172a, 1);
    this.add.text(this.deckPoint.x - 16, this.deckPoint.y - 10, "DECK", {
      fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace",
      fontSize: "11px",
      color: "#dbeafe"
    });

    const latestActionByUser = new Map<number, { action: string; amount: number }>();
    for (const action of this.state.game?.recentActions ?? []) {
      latestActionByUser.set(action.userId, { action: action.action, amount: action.amount });
    }

    for (let i = 0; i < seats.length; i++) {
      const angle = (Math.PI * 2 * i) / seats.length - Math.PI / 2;
      const x = cx + Math.cos(angle) * radiusX;
      const y = cy + Math.sin(angle) * radiusY;
      const seat = seats[i];
      const player = seat.player;
      const isTurn = this.state.game?.turn?.userId === player?.userId;

      this.add.circle(x, y, 27, player ? (isTurn ? 0xfbbf24 : 0x1f2937) : 0x6b7280, 1);
      this.add.text(x - 16, y - 10, String(seat.seatNo), {
        fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace",
        fontSize: "14px",
        color: "#f8fafc"
      });
      if (player) {
        this.seatPointsByUserId.set(player.userId, new Phaser.Math.Vector2(x, y));
        const isBot = player.userId >= 900000000000;
        this.add.text(x - 40, y + 34, `${player.nickname}${isBot ? " [BOT]" : ""}`, {
          fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace",
          fontSize: "12px",
          color: "#ecfeff"
        });
        this.add.text(x - 40, y + 50, `${player.chipStack} chips`, {
          fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace",
          fontSize: "11px",
          color: "#a7f3d0"
        });

        const latest = latestActionByUser.get(player.userId);
        if (latest) {
          this.add.rectangle(x, y - 36, 82, 18, 0x0b1220, 0.88);
          this.add.text(x - 36, y - 42, actionSummary(latest.action, latest.amount), {
            fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace",
            fontSize: "10px",
            color: "#fef3c7"
          });
        }
      }
    }

    const board = this.state.game?.communityCards ?? [];
    const boardStartX = width / 2 - 145;
    const boardY = height / 2 - 30;
    for (let i = 0; i < 5; i++) {
      const x = boardStartX + i * 58;
      this.boardPoints.push(new Phaser.Math.Vector2(x, boardY));
      this.drawPokerCard(x, boardY, board[i]);
    }

    this.add.text(width / 2 - 140, height / 2 + 4, `Pot: ${this.state.game?.mainPot ?? 0}`, {
      fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace",
      fontSize: "15px",
      color: "#bbf7d0"
    });
    this.add.text(width / 2 - 140, height / 2 + 26, `Phase: ${this.state.game?.phase ?? "WAITING"}`, {
      fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace",
      fontSize: "14px",
      color: "#dcfce7"
    });
  }

  private animateDiff(prevGame: GameSnapshotResponse | null, nextGame: GameSnapshotResponse | null): void {
    if (!nextGame) {
      return;
    }
    this.animateBoardDeals(prevGame, nextGame);
    this.animateChipAction(prevGame, nextGame);
    this.animatePotToWinner(prevGame, nextGame);
  }

  private animateBoardDeals(prevGame: GameSnapshotResponse | null, nextGame: GameSnapshotResponse): void {
    const prevCount = prevGame?.communityCards.length ?? 0;
    const nextCount = nextGame.communityCards.length;
    if (nextCount <= prevCount) {
      return;
    }
    for (let i = prevCount; i < nextCount; i++) {
      const target = this.boardPoints[i];
      if (!target) {
        continue;
      }
      const card = this.add.rectangle(this.deckPoint.x, this.deckPoint.y, 52, 66, 0xf8fafc, 1);
      card.setStrokeStyle(1, 0xcbd5e1, 1);
      this.tweens.add({
        targets: [card],
        x: target.x,
        y: target.y,
        duration: 320 + i * 90,
        ease: "Cubic.Out",
        onComplete: () => {
          card.destroy();
          this.drawPokerCard(target.x, target.y, nextGame.communityCards[i]);
        }
      });
    }
  }

  private drawPokerCard(x: number, y: number, code?: string): void {
    const width = 52;
    const height = 66;
    const parsed = code ? parseCard(code) : null;

    if (!parsed) {
      this.add.rectangle(x, y, width, height, 0x1e293b, 1);
      this.add.rectangle(x, y, width, height).setStrokeStyle(1, 0x475569, 1);
      this.add.text(x - 12, y - 8, "--", {
        fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace",
        fontSize: "14px",
        color: "#64748b"
      });
      return;
    }

    const color = parsed.red ? "#b91c1c" : "#0f172a";
    const symbol = suitSymbol(parsed.suit);

    this.add.rectangle(x, y, width, height, 0xf8fafc, 1);
    this.add.rectangle(x, y, width, height).setStrokeStyle(1, 0xcbd5e1, 1);
    this.add.text(x - 20, y - 28, parsed.rank, {
      fontFamily: "Georgia, Times New Roman, serif",
      fontSize: "12px",
      color
    });
    this.add.text(x - 19, y - 16, symbol, {
      fontFamily: "Georgia, Times New Roman, serif",
      fontSize: "11px",
      color
    });
    this.add.text(x - 8, y - 10, symbol, {
      fontFamily: "Georgia, Times New Roman, serif",
      fontSize: "24px",
      color
    });
  }

  private animateChipAction(prevGame: GameSnapshotResponse | null, nextGame: GameSnapshotResponse): void {
    const prevActions = prevGame?.recentActions ?? [];
    const nextActions = nextGame.recentActions ?? [];
    if (nextActions.length <= prevActions.length) {
      return;
    }
    const action = nextActions[nextActions.length - 1];
    if (!action || (action.action !== "CALL" && action.action !== "RAISE")) {
      return;
    }
    const from = this.seatPointsByUserId.get(action.userId);
    if (!from) {
      return;
    }
    const chip = this.add.circle(from.x, from.y, 8, 0xfbbf24, 1);
    const amount = this.add.text(from.x + 10, from.y - 8, String(action.amount), {
      fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace",
      fontSize: "11px",
      color: "#fde68a"
    });
    this.tweens.add({
      targets: [chip, amount],
      x: this.potPoint.x,
      y: this.potPoint.y,
      duration: 360,
      ease: "Sine.InOut",
      onComplete: () => {
        chip.destroy();
        amount.destroy();
      }
    });
  }

  private animatePotToWinner(prevGame: GameSnapshotResponse | null, nextGame: GameSnapshotResponse): void {
    if (!nextGame.finished || !nextGame.winnerUserId) {
      return;
    }
    if (prevGame?.finished && prevGame.winnerUserId === nextGame.winnerUserId) {
      return;
    }
    const winnerPoint = this.seatPointsByUserId.get(nextGame.winnerUserId);
    if (!winnerPoint) {
      return;
    }
    const potBall = this.add.circle(this.potPoint.x, this.potPoint.y, 10, 0x22d3ee, 1);
    const potText = this.add.text(this.potPoint.x + 12, this.potPoint.y - 10, `+${nextGame.mainPot}`, {
      fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace",
      fontSize: "12px",
      color: "#67e8f9"
    });
    this.tweens.add({
      targets: [potBall, potText],
      x: winnerPoint.x,
      y: winnerPoint.y,
      duration: 600,
      ease: "Back.Out",
      onComplete: () => {
        potBall.destroy();
        potText.destroy();
      }
    });
  }
}
