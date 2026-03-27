package com.example.holdem.table.application;

import com.example.holdem.common.error.BusinessException;
import com.example.holdem.common.error.ErrorCode;
import com.example.holdem.table.domain.Table;
import com.example.holdem.table.infrastructure.redis.TableRedisRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TableQueryService {
    private final TableRedisRepository tableRedisRepository;

    public TableQueryService(TableRedisRepository tableRedisRepository) {
        this.tableRedisRepository = tableRedisRepository;
    }

    public List<Table> findAll() {
        return tableRedisRepository.findAll();
    }

    public Table findById(String tableId) {
        return tableRedisRepository.findById(tableId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_NOT_FOUND));
    }
}
