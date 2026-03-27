import { useEffect, useRef } from "react";
import Phaser from "phaser";
import type { GameSnapshotResponse, TableSnapshotResponse } from "../types/api";
import { TableScene } from "../phaser/TableScene";

type Props = {
  table: TableSnapshotResponse | null;
  game: GameSnapshotResponse | null;
};

export default function PhaserTable({ table, game }: Props) {
  const rootRef = useRef<HTMLDivElement | null>(null);
  const gameRef = useRef<Phaser.Game | null>(null);
  const sceneRef = useRef<TableScene | null>(null);

  useEffect(() => {
    if (!rootRef.current || gameRef.current) {
      return;
    }
    const scene = new TableScene();
    sceneRef.current = scene;
    const phaser = new Phaser.Game({
      type: Phaser.AUTO,
      width: 980,
      height: 620,
      parent: rootRef.current,
      scene,
      backgroundColor: "#081a14"
    });
    gameRef.current = phaser;

    return () => {
      phaser.destroy(true);
      gameRef.current = null;
      sceneRef.current = null;
    };
  }, []);

  useEffect(() => {
    sceneRef.current?.updateState({ table, game });
  }, [table, game]);

  return <div className="phaser-root" ref={rootRef} />;
}
