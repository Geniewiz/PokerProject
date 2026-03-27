package com.example.holdem.table.domain;

import java.util.ArrayList;
import java.util.List;

public final class TablePosition {
    private TablePosition() {
    }

    public static List<String> labelsForPlayerCount(int playerCount) {
        if (playerCount < 2 || playerCount > 11) {
            throw new IllegalArgumentException("Player count must be between 2 and 11.");
        }
        if (playerCount == 2) {
            return List.of("DEALER_SMALL_BLIND", "BIG_BLIND");
        }

        List<String> labels = new ArrayList<>();
        labels.add("DEALER");
        labels.add("SMALL_BLIND");
        labels.add("BIG_BLIND");
        int remaining = playerCount - 3;
        if (remaining <= 0) {
            return List.copyOf(labels);
        }

        List<String> trailingPositions = new ArrayList<>();
        if (remaining >= 1) {
            trailingPositions.add(0, "CUTOFF");
        }
        if (remaining >= 2) {
            trailingPositions.add(0, "HIJACK");
        }
        if (remaining >= 3) {
            trailingPositions.add(0, "LOJACK");
        }

        int utgCount = remaining - trailingPositions.size();
        for (int i = 0; i < utgCount; i++) {
            labels.add(i == 0 ? "UTG" : "UTG+" + i);
        }
        labels.addAll(trailingPositions);
        return List.copyOf(labels);
    }
}
