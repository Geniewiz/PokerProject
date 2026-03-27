export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
}

export interface TablePlayerResponse {
  userId: number;
  nickname: string;
  chipStack: number;
  connected: boolean;
  ready: boolean;
}

export interface SeatResponse {
  seatNo: number;
  occupied: boolean;
  player: TablePlayerResponse | null;
}

export interface TableSnapshotResponse {
  tableId: string;
  name: string;
  status: string;
  smallBlind: number;
  bigBlind: number;
  dealerSeatNo: number | null;
  seatedPlayerCount: number;
  seats: SeatResponse[];
}

export interface TurnResponse {
  userId: number;
  callAmount: number;
  minimumRaiseToAmount: number;
}

export interface AvailableActionsResponse {
  canFold: boolean;
  canCheck: boolean;
  canCall: boolean;
  canRaise: boolean;
  callAmount: number;
  minimumRaiseToAmount: number;
  maximumRaiseToAmount: number;
}

export interface PlayerSnapshotResponse {
  userId: number;
  seatNo: number;
  position: string;
  stack: number;
  folded: boolean;
}

export interface PlayerActionResponse {
  userId: number;
  phase: string;
  action: string;
  amount: number;
}

export interface GameSnapshotResponse {
  tableId: string;
  handId: string;
  phase: string;
  finished: boolean;
  dealerUserId: number | null;
  winnerUserId: number | null;
  mainPot: number;
  currentBet: number;
  communityCards: string[];
  turn: TurnResponse | null;
  availableActions: AvailableActionsResponse | null;
  players: Record<string, PlayerSnapshotResponse>;
  recentActions: PlayerActionResponse[];
  myHoleCards: string[];
  showdownResults: ShowdownPlayerResultResponse[];
}

export interface ActionResultMessage {
  tableId: string;
  userId: number | null;
  action: string;
  status: string;
  snapshot: GameSnapshotResponse | null;
}

export interface ShowdownPlayerResultResponse {
  userId: number;
  holeCards: string[];
  handRank: string;
  winner: boolean;
}
