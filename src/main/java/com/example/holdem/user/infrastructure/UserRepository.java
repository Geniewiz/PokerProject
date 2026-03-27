package com.example.holdem.user.infrastructure;

import com.example.holdem.user.domain.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    User save(User user);
}
