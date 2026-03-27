package com.example.holdem.auth.application;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    public String issueSessionToken(String username) {
        return "session-" + username + "-" + UUID.randomUUID();
    }
}
