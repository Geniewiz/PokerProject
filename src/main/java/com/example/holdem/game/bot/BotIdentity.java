package com.example.holdem.game.bot;

public final class BotIdentity {
    public static final long BOT_USER_ID_BASE = 900000000000L;

    private BotIdentity() {
    }

    public static boolean isBot(Long userId) {
        return userId != null && userId >= BOT_USER_ID_BASE;
    }
}
