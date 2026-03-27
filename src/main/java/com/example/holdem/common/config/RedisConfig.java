package com.example.holdem.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

@Configuration
@ConditionalOnProperty(prefix = "holdem.redis", name = "enabled", havingValue = "true")
@EnableRedisRepositories
@EnableRedisIndexedHttpSession
public class RedisConfig {
}
