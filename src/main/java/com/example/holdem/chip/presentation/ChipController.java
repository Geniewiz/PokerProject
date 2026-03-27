package com.example.holdem.chip.presentation;

import com.example.holdem.chip.application.BuyInService;
import com.example.holdem.common.response.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chips")
public class ChipController {
    private final BuyInService buyInService;

    public ChipController(BuyInService buyInService) {
        this.buyInService = buyInService;
    }

    @PostMapping("/{userId}/buy-in")
    public ApiResponse<Long> buyIn(@PathVariable Long userId, @RequestParam long amount) {
        return ApiResponse.ok(buyInService.buyIn(userId, amount));
    }
}
