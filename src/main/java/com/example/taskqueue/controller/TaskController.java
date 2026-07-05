package com.example.taskqueue.controller;

import com.example.taskqueue.data.TaskJob;
import com.example.taskqueue.data.TaskRequest;
import com.example.taskqueue.service.TaskQueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {

    private final TaskQueueService taskQueueService;

    // Enqueue a new task
    @PostMapping("/enqueue")
    public ResponseEntity<Map<String, Object>> enqueue(@Valid @RequestBody TaskRequest request) {
        TaskJob job = taskQueueService.enqueue(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "jobId", job.getId(),
                "status", job.getStatus(),
                "message", "Task accepted and queued for execution"
        ));
    }

    // Poll job status
    @GetMapping("/job/{id}")
    public ResponseEntity<TaskJob> getJobStatus(@PathVariable String id) {
        return taskQueueService.getJob(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
