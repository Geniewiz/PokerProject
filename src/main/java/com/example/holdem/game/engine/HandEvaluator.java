package com.example.holdem.game.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandEvaluator {
    public record EvaluatedHand(long score, String handRank) {
    }

    public long evaluate(List<Card> cards) {
        return evaluateWithRank(cards).score();
    }

    public EvaluatedHand evaluateWithRank(List<Card> cards) {
        if (cards.size() < 5) {
            throw new IllegalArgumentException("At least five cards are required.");
        }
        long best = Long.MIN_VALUE;
        int bestCategory = -1;
        for (int a = 0; a < cards.size() - 4; a++) {
            for (int b = a + 1; b < cards.size() - 3; b++) {
                for (int c = b + 1; c < cards.size() - 2; c++) {
                    for (int d = c + 1; d < cards.size() - 1; d++) {
                        for (int e = d + 1; e < cards.size(); e++) {
                            FiveCardEvaluation evaluation = evaluateFiveCards(List.of(cards.get(a), cards.get(b), cards.get(c), cards.get(d), cards.get(e)));
                            if (evaluation.score() > best) {
                                best = evaluation.score();
                                bestCategory = evaluation.category();
                            }
                        }
                    }
                }
            }
        }
        return new EvaluatedHand(best, categoryName(bestCategory));
    }

    private record FiveCardEvaluation(long score, int category) {
    }

    private FiveCardEvaluation evaluateFiveCards(List<Card> cards) {
        List<Integer> ranks = cards.stream()
                .map(card -> card.rank().value())
                .sorted(Comparator.reverseOrder())
                .toList();
        Map<Integer, Integer> counts = new HashMap<>();
        Map<Card.Suit, Integer> suitCounts = new EnumMap<>(Card.Suit.class);
        for (Card card : cards) {
            counts.merge(card.rank().value(), 1, Integer::sum);
            suitCounts.merge(card.suit(), 1, Integer::sum);
        }

        boolean flush = suitCounts.values().stream().anyMatch(count -> count == 5);
        int straightHigh = straightHigh(ranks);
        List<Map.Entry<Integer, Integer>> grouped = counts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.<Integer, Integer>comparingByKey().reversed()))
                .toList();

        if (straightHigh > 0 && flush) {
            return new FiveCardEvaluation(score(8, List.of(straightHigh)), 8);
        }
        if (grouped.get(0).getValue() == 4) {
            return new FiveCardEvaluation(score(7, List.of(grouped.get(0).getKey(), grouped.get(1).getKey())), 7);
        }
        if (grouped.get(0).getValue() == 3 && grouped.get(1).getValue() == 2) {
            return new FiveCardEvaluation(score(6, List.of(grouped.get(0).getKey(), grouped.get(1).getKey())), 6);
        }
        if (flush) {
            return new FiveCardEvaluation(score(5, ranks), 5);
        }
        if (straightHigh > 0) {
            return new FiveCardEvaluation(score(4, List.of(straightHigh)), 4);
        }
        if (grouped.get(0).getValue() == 3) {
            List<Integer> kickers = grouped.stream().skip(1).map(Map.Entry::getKey).toList();
            List<Integer> values = new ArrayList<>();
            values.add(grouped.get(0).getKey());
            values.addAll(kickers);
            return new FiveCardEvaluation(score(3, values), 3);
        }
        if (grouped.get(0).getValue() == 2 && grouped.get(1).getValue() == 2) {
            return new FiveCardEvaluation(score(2, List.of(grouped.get(0).getKey(), grouped.get(1).getKey(), grouped.get(2).getKey())), 2);
        }
        if (grouped.get(0).getValue() == 2) {
            List<Integer> values = new ArrayList<>();
            values.add(grouped.get(0).getKey());
            values.addAll(grouped.stream().skip(1).map(Map.Entry::getKey).toList());
            return new FiveCardEvaluation(score(1, values), 1);
        }
        return new FiveCardEvaluation(score(0, ranks), 0);
    }

    private int straightHigh(List<Integer> descendingRanks) {
        List<Integer> unique = descendingRanks.stream().distinct().sorted(Comparator.reverseOrder()).toList();
        if (unique.equals(List.of(14, 5, 4, 3, 2))) {
            return 5;
        }
        for (int i = 0; i <= unique.size() - 5; i++) {
            int start = unique.get(i);
            boolean straight = true;
            for (int j = 1; j < 5; j++) {
                if (unique.get(i + j) != start - j) {
                    straight = false;
                    break;
                }
            }
            if (straight) {
                return start;
            }
        }
        return 0;
    }

    private long score(int category, List<Integer> values) {
        long result = category;
        for (Integer value : values) {
            result = (result << 4) | value;
        }
        return result;
    }

    private String categoryName(int category) {
        return switch (category) {
            case 8 -> "STRAIGHT_FLUSH";
            case 7 -> "FOUR_OF_A_KIND";
            case 6 -> "FULL_HOUSE";
            case 5 -> "FLUSH";
            case 4 -> "STRAIGHT";
            case 3 -> "THREE_OF_A_KIND";
            case 2 -> "TWO_PAIR";
            case 1 -> "ONE_PAIR";
            case 0 -> "HIGH_CARD";
            default -> "UNKNOWN";
        };
    }
}
