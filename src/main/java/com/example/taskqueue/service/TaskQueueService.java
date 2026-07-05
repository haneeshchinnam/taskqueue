package com.example.taskqueue.service;

import com.example.taskqueue.data.TaskJob;
import com.example.taskqueue.data.TaskRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskQueueService {

    private final Map<String, TaskJob> jobRegistry = new ConcurrentHashMap<>();
    final LinkedBlockingQueue<TaskJob> queue = new LinkedBlockingQueue<>();

    private final TaskExecutorService executorService;

    public TaskJob enqueue(TaskRequest request) {
        log.info(request.getUrl());
        TaskJob job = TaskJob.builder()
                .url(request.getUrl())
                .payload(request.getPayload())
                .httpMethod(HttpMethod.valueOf(request.getMethod()))
                .headers(request.getHeaders() != null ? request.getHeaders() : Map.of())
                .build();

        jobRegistry.put(job.getId(), job);
        queue.offer(job);
        executorService.startProcessing(this);

        log.info("Job enqueued: {}", job.getId());
        return job;
    }

    public Optional<TaskJob> getJob(String id) {
        return Optional.ofNullable(jobRegistry.get(id));
    }
}
