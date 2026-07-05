package com.example.taskqueue.service;

import com.example.taskqueue.config.RetryConfig;
import com.example.taskqueue.data.TaskJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutorService {

    private final RetryConfig retryConfig;
    private final WebClient webClient = WebClient.builder().build();

    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(4);

    // Kick off queue processing (idempotent)
    public void startProcessing(TaskQueueService queueService) {
        if (isProcessing.compareAndSet(false, true)) {
            scheduler.execute(() -> drainQueue(queueService));
        }
    }

    private void drainQueue(TaskQueueService queueService) {
        try {
            while (true) {
                TaskJob job = queueService.queue.poll();
                if (job == null) break;
                executeJob(job, queueService);
            }
        } finally {
            isProcessing.set(false);
            // Handle race: new jobs may have arrived just before flag reset
            if (!queueService.queue.isEmpty()) {
                startProcessing(queueService);
            }
        }
    }

    private void executeJob(TaskJob job, TaskQueueService queueService) {
        job.setAttempt(job.getAttempt() + 1);
        job.setStatus(TaskJob.Status.RUNNING);
        job.setLastAttemptAt(Instant.now());

        log.info("Executing job {} | attempt {}/{}", job.getId(),
                job.getAttempt(), retryConfig.getMaxAttempts());

        try {
            webClient.method(job.getHttpMethod())
                    .uri(job.getUrl())
                    .headers(h -> job.getHeaders().forEach(h::add))
                    .bodyValue(job.getPayload())
                    .retrieve()
                    // Treat 4xx/5xx as errors
                    .onStatus(status -> !status.is2xxSuccessful(),
                            resp -> resp.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new RuntimeException("HTTP " + resp.statusCode() + ": " + body))))
                    .bodyToMono(String.class)
                    .block(); // Block here since we're managing threading ourselves

            // ✅ Success
            job.setStatus(TaskJob.Status.SUCCESS);
            job.setCompletedAt(Instant.now());
            log.info("✅ Job {} succeeded on attempt {}", job.getId(), job.getAttempt());

        } catch (Exception ex) {
            job.setLastError(ex.getMessage());
            log.warn("⚠️  Job {} attempt {} failed: {}", job.getId(), job.getAttempt(), ex.getMessage());

            if (job.getAttempt() >= retryConfig.getMaxAttempts()) {
                // ❌ Permanently failed
                job.setStatus(TaskJob.Status.FAILED);
                log.error("❌ Job {} permanently failed after {} attempts", job.getId(), job.getAttempt());
            } else {
                // 🔄 Schedule retry with exponential backoff
                long delayMs = retryConfig.getBackoffDelay(job.getAttempt());
                job.setStatus(TaskJob.Status.RETRYING);
                job.setNextRetryAt(Instant.now().plusMillis(delayMs));

                log.info("🔄 Job {} will retry in {}s (attempt {}/{})",
                        job.getId(),
                        delayMs / 1000,
                        job.getAttempt() + 1,
                        retryConfig.getMaxAttempts());

                scheduler.schedule(() -> {
                    queueService.queue.offer(job);
                    startProcessing(queueService);
                }, delayMs, TimeUnit.MILLISECONDS);
            }
        }
    }
}
