package com.example.taskqueue.data;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpMethod;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class TaskJob {

    public enum Status { PENDING, RUNNING, RETRYING, SUCCESS, FAILED }

    @Builder.Default
    private String id = java.util.UUID.randomUUID().toString();

    private String url;

    private Object payload;
    private Map<String, String> headers;

    private HttpMethod httpMethod;

    @Builder.Default
    private int attempt = 0;

    @Builder.Default
    private Status status = Status.PENDING;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant lastAttemptAt;
    private Instant nextRetryAt;
    private Instant completedAt;
    private String lastError;
}
