package com.example.holdem.game.engine;

import com.example.holdem.game.domain.Pot;
import com.example.holdem.game.domain.SidePot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PotCalculator {

    public Pot calculate(Map<Long, Long> committedChips, Set<Long> foldedPlayerIds) {
        List<Long> levels = committedChips.values().stream()
                .filter(amount -> amount > 0)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        if (levels.isEmpty()) {
            return new Pot(0, List.of());
        }

        long previous = 0;
        List<SidePot> pots = new ArrayList<>();
        for (Long level : levels) {
            long delta = level - previous;
            if (delta <= 0) {
                continue;
            }

            List<Long> contributors = committedChips.entrySet().stream()
                    .filter(entry -> entry.getValue() >= level)
                    .map(Map.Entry::getKey)
                    .toList();
            long amount = delta * contributors.size();
            if (amount <= 0) {
                previous = level;
                continue;
            }

            Set<Long> eligible = contributors.stream()
                    .filter(userId -> !foldedPlayerIds.contains(userId))
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if (!eligible.isEmpty()) {
                pots.add(new SidePot(amount, Set.copyOf(eligible)));
            }
            previous = level;
        }

        if (pots.isEmpty()) {
            return new Pot(0, List.of());
        }
        SidePot main = pots.get(0);
        List<SidePot> side = pots.size() == 1 ? List.of() : pots.subList(1, pots.size());
        return new Pot(main.amount(), List.copyOf(side));
    }

    public long total(Pot pot) {
        return pot.mainPot() + pot.sidePots().stream().mapToLong(SidePot::amount).sum();
    }
}
