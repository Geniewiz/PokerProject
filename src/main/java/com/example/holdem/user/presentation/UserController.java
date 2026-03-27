package com.example.holdem.user.presentation;

import com.example.holdem.common.response.ApiResponse;
import com.example.holdem.user.application.UserQueryService;
import com.example.holdem.user.domain.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserQueryService userQueryService;

    public UserController(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @GetMapping("/{userId}")
    public ApiResponse<User> getUser(@PathVariable Long userId) {
        return ApiResponse.ok(userQueryService.getById(userId));
    }
}
