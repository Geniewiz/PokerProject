package com.example.holdem.game.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.holdem.game.bot.BotIdentity;
import com.example.holdem.history.infrastructure.HandHistoryJpaRepository;
import com.example.holdem.table.application.TableCommandService;
import com.example.holdem.table.application.TableQueryService;
import com.example.holdem.table.domain.Table;
import com.example.holdem.table.presentation.rest.dto.CreateTableRequest;
import com.example.holdem.table.presentation.ws.inbound.ActionCommand;
import com.example.holdem.table.presentation.ws.inbound.SitCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GameFlowIntegrationTest {

    @Autowired
    private TableCommandService tableCommandService;

    @Autowired
    private StartHandUseCase startHandUseCase;

    @Autowired
    private ApplyActionUseCase applyActionUseCase;

    @Autowired
    private HandHistoryJpaRepository handHistoryJpaRepository;

    @Autowired
    private TableQueryService tableQueryService;

    @Test
    void startHandPostsBlindsAndSetsFirstTurn() {
        Table table = tableCommandService.createTable(new CreateTableRequest("heads-up", 2, 50, 100));
        tableCommandService.sit(new SitCommand(table.tableId(), 1L, 1, 1_000));
        tableCommandService.sit(new SitCommand(table.tableId(), 2L, 2, 1_000));

        var hand = startHandUseCase.start(table.tableId());

        assertThat(hand.phase().name()).isEqualTo("PREFLOP");
        assertThat(hand.pot().mainPot()).isEqualTo(150);
        assertThat(hand.playerTurn().userId()).isEqualTo(1L);
        assertThat(hand.playerTurn().callAmount()).isEqualTo(50);
        assertThat(hand.stackByPlayer()).containsEntry(1L, 950L).containsEntry(2L, 900L);
        assertThat(hand.communityCards().revealedCount()).isZero();
    }

    @Test
    void raiseAndCallsAdvanceStreetToFlop() {
        Table table = tableCommandService.createTable(new CreateTableRequest("three-way", 3, 50, 100));
        tableCommandService.sit(new SitCommand(table.tableId(), 1L, 1, 1_000));
        tableCommandService.sit(new SitCommand(table.tableId(), 2L, 2, 1_000));
        tableCommandService.sit(new SitCommand(table.tableId(), 3L, 3, 1_000));

        startHandUseCase.start(table.tableId());
        applyActionUseCase.apply(new ActionCommand(table.tableId(), 1L, "RAISE", 200));
        applyActionUseCase.apply(new ActionCommand(table.tableId(), 2L, "CALL", 150));
        var hand = applyActionUseCase.apply(new ActionCommand(table.tableId(), 3L, "CALL", 100));

        assertThat(hand.phase().name()).isEqualTo("FLOP");
        assertThat(hand.communityCards().revealedCount()).isEqualTo(3);
        assertThat(hand.playerTurn().userId()).isEqualTo(2L);
        assertThat(hand.playerTurn().callAmount()).isZero();
        assertThat(hand.stackByPlayer()).containsEntry(1L, 800L).containsEntry(2L, 800L).containsEntry(3L, 800L);
        assertThat(hand.pot().mainPot()).isEqualTo(600);
    }

    @Test
    void foldLeavesLastPlayerAsWinnerAndPersistsHistory() {
        long before = handHistoryJpaRepository.count();

        Table table = tableCommandService.createTable(new CreateTableRequest("finish-by-fold", 2, 50, 100));
        tableCommandService.sit(new SitCommand(table.tableId(), 11L, 1, 1_000));
        tableCommandService.sit(new SitCommand(table.tableId(), 22L, 2, 1_000));

        startHandUseCase.start(table.tableId());
        var hand = applyActionUseCase.apply(new ActionCommand(table.tableId(), 11L, "FOLD", 0));

        assertThat(hand.isFinished()).isTrue();
        assertThat(hand.winnerUserId()).isEqualTo(22L);
        assertThat(hand.stackByPlayer()).containsEntry(11L, 950L).containsEntry(22L, 1_050L);
        assertThat(handHistoryJpaRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void positionMappingSupportsElevenPlayers() {
        Table table = tableCommandService.createTable(new CreateTableRequest("eleven-max", 11, 50, 100));
        for (int i = 1; i <= 11; i++) {
            tableCommandService.sit(new SitCommand(table.tableId(), (long) i, i, 1_000));
        }

        var hand = startHandUseCase.start(table.tableId());

        assertThat(hand.players().get(1L).position()).isEqualTo("DEALER");
        assertThat(hand.players().get(2L).position()).isEqualTo("SMALL_BLIND");
        assertThat(hand.players().get(3L).position()).isEqualTo("BIG_BLIND");
        assertThat(hand.players().get(4L).position()).isEqualTo("UTG");
        assertThat(hand.players().get(5L).position()).isEqualTo("UTG+1");
        assertThat(hand.players().get(6L).position()).isEqualTo("UTG+2");
        assertThat(hand.players().get(7L).position()).isEqualTo("UTG+3");
        assertThat(hand.players().get(8L).position()).isEqualTo("UTG+4");
        assertThat(hand.players().get(9L).position()).isEqualTo("LOJACK");
        assertThat(hand.players().get(10L).position()).isEqualTo("HIJACK");
        assertThat(hand.players().get(11L).position()).isEqualTo("CUTOFF");
    }

    @Test
    void dealerButtonMovesToNextOccupiedSeatOnNextHand() {
        Table table = tableCommandService.createTable(new CreateTableRequest("rotation", 11, 50, 100));
        tableCommandService.sit(new SitCommand(table.tableId(), 100L, 2, 1_000));
        tableCommandService.sit(new SitCommand(table.tableId(), 200L, 5, 1_000));
        tableCommandService.sit(new SitCommand(table.tableId(), 300L, 9, 1_000));

        var firstHand = startHandUseCase.start(table.tableId());
        var afterFirstStart = tableQueryService.findById(table.tableId());
        var secondHand = startHandUseCase.start(table.tableId());
        var afterSecondStart = tableQueryService.findById(table.tableId());

        assertThat(firstHand.dealerUserId()).isEqualTo(100L);
        assertThat(afterFirstStart.dealerSeatNo()).isEqualTo(2);
        assertThat(secondHand.dealerUserId()).isEqualTo(200L);
        assertThat(afterSecondStart.dealerSeatNo()).isEqualTo(5);
        assertThat(secondHand.players().get(200L).position()).isEqualTo("DEALER");
        assertThat(secondHand.players().get(300L).position()).isEqualTo("SMALL_BLIND");
        assertThat(secondHand.players().get(100L).position()).isEqualTo("BIG_BLIND");
    }

    @Test
    void oneHumanCanStartWithBotAndBotActsFromBackend() {
        Table table = tableCommandService.createTable(new CreateTableRequest("solo-plus-bot", 11, 50, 100));
        tableCommandService.sit(new SitCommand(table.tableId(), 500L, 1, 1_000));

        var hand = startHandUseCase.start(table.tableId(), true, 1);

        assertThat(hand.players()).hasSize(2);
        Long botUserId = hand.turnOrder().stream().filter(BotIdentity::isBot).findFirst().orElseThrow();
        assertThat(hand.players().get(botUserId)).isNotNull();
        assertThat(hand.players().get(botUserId).position()).isIn("SMALL_BLIND", "BIG_BLIND", "DEALER_SMALL_BLIND", "DEALER");

        if (!hand.isFinished()) {
            assertThat(hand.playerTurn()).isNotNull();
            assertThat(BotIdentity.isBot(hand.playerTurn().userId())).isFalse();
        }
    }

    @Test
    void oneHumanCanAddMultipleBotsByBotCount() {
        Table table = tableCommandService.createTable(new CreateTableRequest("solo-plus-3bots", 11, 50, 100));
        tableCommandService.sit(new SitCommand(table.tableId(), 700L, 1, 1_000));

        var hand = startHandUseCase.start(table.tableId(), true, 3);

        long botPlayers = hand.turnOrder().stream().filter(BotIdentity::isBot).count();
        assertThat(hand.players()).hasSize(4);
        assertThat(botPlayers).isEqualTo(3);
        assertThat(hand.players().get(700L)).isNotNull();
    }

    @Test
    void botCountRespectsElevenPlayerLimit() {
        Table table = tableCommandService.createTable(new CreateTableRequest("cap-check", 11, 50, 100));
        for (int i = 1; i <= 10; i++) {
            tableCommandService.sit(new SitCommand(table.tableId(), 800L + i, i, 1_000));
        }

        var hand = startHandUseCase.start(table.tableId(), true, 5);

        long botPlayers = hand.turnOrder().stream().filter(BotIdentity::isBot).count();
        assertThat(hand.players()).hasSize(11);
        assertThat(botPlayers).isEqualTo(1);
    }
}
