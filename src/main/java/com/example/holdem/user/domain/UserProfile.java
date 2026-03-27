package com.example.holdem.user.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public class UserProfile {
    private String nickname;
    private String avatarUrl;

    protected UserProfile() {
    }

    public UserProfile(String nickname, String avatarUrl) {
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
