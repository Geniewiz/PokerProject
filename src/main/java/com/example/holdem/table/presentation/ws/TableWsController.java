package com.example.holdem.table.presentation.ws;

import com.example.holdem.game.application.ApplyActionUseCase;
import com.example.holdem.table.application.TableCommandService;
import com.example.holdem.table.presentation.ws.inbound.ActionCommand;
import com.example.holdem.table.presentation.ws.inbound.LeaveCommand;
import com.example.holdem.table.presentation.ws.inbound.SitCommand;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class TableWsController {
    private final TableCommandService tableCommandService;
    private final ApplyActionUseCase applyActionUseCase;

    public TableWsController(TableCommandService tableCommandService, ApplyActionUseCase applyActionUseCase) {
        this.tableCommandService = tableCommandService;
        this.applyActionUseCase = applyActionUseCase;
    }

    @MessageMapping("/table.sit")
    public void sit(SitCommand command) {
        tableCommandService.sit(command);
    }

    @MessageMapping("/table.leave")
    public void leave(LeaveCommand command) {
        tableCommandService.leave(command);
    }

    @MessageMapping("/table.action")
    public void action(ActionCommand command) {
        applyActionUseCase.apply(command);
    }
}
