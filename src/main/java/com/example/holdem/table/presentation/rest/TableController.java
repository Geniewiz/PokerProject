package com.example.holdem.table.presentation.rest;

import com.example.holdem.common.response.ApiResponse;
import com.example.holdem.table.application.TableCommandService;
import com.example.holdem.table.application.TableQueryService;
import com.example.holdem.table.presentation.rest.dto.CreateTableRequest;
import com.example.holdem.table.presentation.rest.dto.SitTableRequest;
import com.example.holdem.table.presentation.rest.dto.TableSnapshotMapper;
import com.example.holdem.table.presentation.rest.dto.TableSnapshotResponse;
import com.example.holdem.table.presentation.ws.inbound.SitCommand;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tables")
public class TableController {
    private final TableCommandService tableCommandService;
    private final TableQueryService tableQueryService;
    private final TableSnapshotMapper tableSnapshotMapper;

    public TableController(
            TableCommandService tableCommandService,
            TableQueryService tableQueryService,
            TableSnapshotMapper tableSnapshotMapper
    ) {
        this.tableCommandService = tableCommandService;
        this.tableQueryService = tableQueryService;
        this.tableSnapshotMapper = tableSnapshotMapper;
    }

    @PostMapping
    public ApiResponse<TableSnapshotResponse> create(@Valid @RequestBody CreateTableRequest request) {
        return ApiResponse.ok(tableSnapshotMapper.from(tableCommandService.createTable(request)));
    }

    @GetMapping
    public ApiResponse<List<TableSnapshotResponse>> getTables() {
        return ApiResponse.ok(tableQueryService.findAll().stream().map(tableSnapshotMapper::from).toList());
    }

    @GetMapping("/{tableId}")
    public ApiResponse<TableSnapshotResponse> getTable(@PathVariable String tableId) {
        return ApiResponse.ok(tableSnapshotMapper.from(tableQueryService.findById(tableId)));
    }

    @PostMapping("/{tableId}/sit")
    public ApiResponse<TableSnapshotResponse> sit(@PathVariable String tableId, @Valid @RequestBody SitTableRequest request) {
        tableCommandService.sit(new SitCommand(tableId, request.userId(), request.seatNo(), request.buyInAmount()));
        return ApiResponse.ok(tableSnapshotMapper.from(tableQueryService.findById(tableId)));
    }
}
