package com.example.taskqueue.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "retry")
public class RetryConfig {

    private int maxAttempts = 5;
    private long minBackoffMs = 1000;
    private long maxBackoffMs = 60_000;
    private int maxDoublings = 4;
    private double jitterFactor = 0.25;

    public long getBackoffDelay(int attempt) {
        int doublings = Math.min(attempt, maxDoublings);
        double delay = minBackoffMs * Math.pow(2, doublings);

        double jitter = delay * jitterFactor * (Math.random() * 2 - 1);
        long finalDelay = (long) Math.min(delay + jitter, maxBackoffMs);

        return Math.max(finalDelay, minBackoffMs);
    }

}
