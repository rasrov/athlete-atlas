package com.athlete.atlas.service;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class ParallelTaskExecutorImpl implements ParallelTaskExecutor {

  private final ExecutorService executorService;

  private final Semaphore upstreamConcurrencyLimiter;

  private final Duration globalTimeout;

  private final ContextSnapshotFactory contextSnapshotFactory =
      ContextSnapshotFactory.builder().build();

  public ParallelTaskExecutorImpl(
      final ExecutorService executorService,
      final Semaphore upstreamConcurrencyLimiter,
      @Value("${rest-clients.product.global-timeout:6s}") final Duration globalTimeout) {
    this.executorService = executorService;
    this.upstreamConcurrencyLimiter = upstreamConcurrencyLimiter;
    this.globalTimeout = globalTimeout;
  }

  @Override
  public <T, R> List<TaskResult<R>> executeInParallel(
      final Collection<T> input, final Function<T, R> task) {
    // The context (MDC, observation, etc.) of the request thread is captured in order to restore it
    // in each executor thread, ensuring that traces and logs from parallel calls remain correlated.
    final ContextSnapshot contextSnapshot = this.contextSnapshotFactory.captureAll();

    final List<CompletableFuture<TaskResult<R>>> futures =
        input.stream().map(item -> this.submit(item, task, contextSnapshot)).toList();

    final List<TaskResult<R>> results = new ArrayList<>(futures.size());
    for (final CompletableFuture<TaskResult<R>> future : futures) {
      results.add(this.join(future));
    }
    return results;
  }

  private <T, R> CompletableFuture<TaskResult<R>> submit(
      final T item, final Function<T, R> task, final ContextSnapshot contextSnapshot) {
    // Backpressure: executes only if a permit is available. If the upstream
    // is saturated (no permits), the task is rejected in a controlled manner instead of being
    // queued indefinitely.
    if (!this.upstreamConcurrencyLimiter.tryAcquire()) {
      log.warn("Upstream concurrency limit reached, rejecting task");
      return CompletableFuture.completedFuture(
          TaskResult.failure(new RejectedExecutionException("Upstream concurrency limit reached")));
    }
    return CompletableFuture.supplyAsync(
        () -> this.runGuarded(item, task, contextSnapshot), this.executorService);
  }

  private <T, R> TaskResult<R> runGuarded(
      final T item, final Function<T, R> task, final ContextSnapshot contextSnapshot) {
    try (final ContextSnapshot.Scope scope = contextSnapshot.setThreadLocals()) {
      return TaskResult.success(task.apply(item));
    } catch (final RuntimeException e) {
      return TaskResult.failure(e);
    } finally {
      this.upstreamConcurrencyLimiter.release();
    }
  }

  private <R> TaskResult<R> join(final CompletableFuture<TaskResult<R>> future) {
    try {
      // Global per-request timeout: prevents indefinite blocking if the upstream hangs.
      return future.get(this.globalTimeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (final TimeoutException e) {
      future.cancel(true);
      log.warn("Task exceeded the global timeout of {}", this.globalTimeout);
      return TaskResult.failure(e);
    } catch (final ExecutionException e) {
      return TaskResult.failure(e.getCause());
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return TaskResult.failure(e);
    }
  }
}
