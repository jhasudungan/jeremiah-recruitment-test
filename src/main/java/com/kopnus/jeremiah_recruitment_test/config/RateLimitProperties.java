package com.kopnus.jeremiah_recruitment_test.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(Limit transfer, Limit login) {


    public record Limit(long capacity, Duration period) {
    }
}