import type {
  ApiResponse,
  GameSnapshotResponse,
  TableSnapshotResponse
} from "../types/api";

const API_BASE = import.meta.env.VITE_API_BASE ?? "";

async function request<T>(
  path: string,
  options: RequestInit,
  userId?: number
): Promise<T> {
  const headers = new Headers(options.headers ?? {});
  headers.set("Content-Type", "application/json");
  if (userId) {
    headers.set("X-USER-ID", String(userId));
  }
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (!response.ok) {
    throw new Error(`Request failed ${response.status}: ${await response.text()}`);
  }
  const payload = (await response.json()) as ApiResponse<T>;
  if (!payload.success) {
    throw new Error(payload.message ?? "API failed");
  }
  return payload.data;
}

export async function createTable(
  userId: number,
  name: string,
  maxPlayers = 11,
  smallBlind = 50,
  bigBlind = 100
): Promise<TableSnapshotResponse> {
  return request<TableSnapshotResponse>(
    "/api/v1/tables",
    {
      method: "POST",
      body: JSON.stringify({ name, maxPlayers, smallBlind, bigBlind })
    },
    userId
  );
}

export async function getTable(userId: number, tableId: string): Promise<TableSnapshotResponse> {
  return request<TableSnapshotResponse>(`/api/v1/tables/${tableId}`, { method: "GET" }, userId);
}

export async function sitAtTable(
  userId: number,
  tableId: string,
  seatNo: number,
  buyInAmount = 1000
): Promise<TableSnapshotResponse> {
  return request<TableSnapshotResponse>(
    `/api/v1/tables/${tableId}/sit`,
    {
      method: "POST",
      body: JSON.stringify({ userId, seatNo, buyInAmount })
    },
    userId
  );
}

export async function startGame(
  userId: number,
  tableId: string,
  withBot: boolean,
  botCount: number
): Promise<GameSnapshotResponse> {
  const query = new URLSearchParams({
    userId: String(userId),
    withBot: String(withBot),
    botCount: String(botCount)
  }).toString();
  return request<GameSnapshotResponse>(
    `/api/v1/games/tables/${tableId}/start?${query}`,
    { method: "POST" },
    userId
  );
}

export async function getGame(userId: number, tableId: string): Promise<GameSnapshotResponse> {
  const query = new URLSearchParams({ userId: String(userId) }).toString();
  return request<GameSnapshotResponse>(`/api/v1/games/tables/${tableId}?${query}`, { method: "GET" }, userId);
}
