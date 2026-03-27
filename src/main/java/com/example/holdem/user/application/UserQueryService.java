package com.example.holdem.user.application;

import com.example.holdem.common.error.BusinessException;
import com.example.holdem.common.error.ErrorCode;
import com.example.holdem.user.domain.User;
import com.example.holdem.user.infrastructure.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserQueryService {
    private final UserRepository userRepository;

    public UserQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
