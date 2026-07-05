package com.example.taskqueue.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.http.HttpMethod;

import java.util.Map;

@Data
public class TaskRequest {

    @NotBlank(message = "url is required")
    private String url;

    @NotNull(message = "payload is required")
    private Object payload;
    private String method;
    private Map<String, String> headers;
}
