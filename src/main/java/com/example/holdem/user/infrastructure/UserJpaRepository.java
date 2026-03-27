package com.example.holdem.user.infrastructure;

import com.example.holdem.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, Long>, UserRepository {
    @Override
    Optional<User> findByUsername(String username);
}
