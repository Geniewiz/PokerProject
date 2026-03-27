import { Client } from "@stomp/stompjs";
import type { ActionResultMessage, TableSnapshotResponse } from "../types/api";

type Handlers = {
  onTableSnapshot: (snapshot: TableSnapshotResponse) => void;
  onActionResult: (message: ActionResultMessage) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
};

export class HoldemWsClient {
  private client: Client | null = null;
  private connected = false;

  connect(tableId: string, handlers: Handlers): void {
    if (this.client?.active) {
      return;
    }

    this.client = new Client({
      reconnectDelay: 3000,
      brokerURL: `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}/ws`,
      onConnect: () => {
        this.connected = true;
        handlers.onConnect?.();
        this.client?.subscribe(`/topic/tables/${tableId}`, (frame) => {
          const payload = JSON.parse(frame.body) as { table: TableSnapshotResponse };
          handlers.onTableSnapshot(payload.table);
        });
        this.client?.subscribe(`/topic/tables/${tableId}/actions`, (frame) => {
          handlers.onActionResult(JSON.parse(frame.body) as ActionResultMessage);
        });
      },
      onStompError: (frame) => {
        console.error("STOMP error", frame.body);
      },
      onWebSocketClose: () => {
        this.connected = false;
        handlers.onDisconnect?.();
      }
    });
    this.client.activate();
  }

  disconnect(): void {
    this.client?.deactivate();
    this.connected = false;
  }

  sendSit(tableId: string, userId: number, seatNo: number, buyInAmount: number): void {
    this.publish("/app/table.sit", { tableId, userId, seatNo, buyInAmount });
  }

  sendAction(tableId: string, userId: number, action: string, amount: number): void {
    this.publish("/app/table.action", { tableId, userId, action, amount });
  }

  private publish(destination: string, payload: object): void {
    if (!this.connected || !this.client) {
      throw new Error("WebSocket not connected");
    }
    this.client.publish({ destination, body: JSON.stringify(payload) });
  }
}
