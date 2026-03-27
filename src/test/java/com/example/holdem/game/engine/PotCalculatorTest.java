package com.example.holdem.game.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PotCalculatorTest {
    private final PotCalculator potCalculator = new PotCalculator();

    @Test
    void calculatesMainAndSidePotsForAllInScenario() {
        var pot = potCalculator.calculate(
                Map.of(
                        1L, 100L,
                        2L, 200L,
                        3L, 500L
                ),
                Set.of()
        );

        assertThat(pot.mainPot()).isEqualTo(300);
        assertThat(pot.sidePots()).hasSize(2);
        assertThat(pot.sidePots().get(0).amount()).isEqualTo(200);
        assertThat(pot.sidePots().get(0).eligiblePlayerIds()).containsExactlyInAnyOrder(2L, 3L);
        assertThat(pot.sidePots().get(1).amount()).isEqualTo(300);
        assertThat(pot.sidePots().get(1).eligiblePlayerIds()).containsExactly(3L);
        assertThat(potCalculator.total(pot)).isEqualTo(800);
    }

    @Test
    void foldedPlayersAreExcludedFromEligibilityOnly() {
        var pot = potCalculator.calculate(
                Map.of(
                        1L, 100L,
                        2L, 200L,
                        3L, 200L
                ),
                Set.of(1L)
        );

        assertThat(pot.mainPot()).isEqualTo(300);
        assertThat(pot.sidePots()).hasSize(1);
        assertThat(pot.sidePots().get(0).amount()).isEqualTo(200);
        assertThat(pot.sidePots().get(0).eligiblePlayerIds()).containsExactlyInAnyOrder(2L, 3L);
    }
}
