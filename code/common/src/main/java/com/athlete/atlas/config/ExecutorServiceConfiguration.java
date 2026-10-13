package com.athlete.atlas.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutorServiceConfiguration {

  /**
   * Executor basado en hilos virtuales: crea un hilo por tarea. Al no haber pool acotado, la
   * presion contra el upstream se limita aparte con {@link #upstreamConcurrencyLimiter(int)}.
   */
  @Bean
  public ExecutorService executorService() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }

  /**
   * Semaforo que acota cuantas llamadas concurrentes pueden ir al upstream. Es el backpressure
   * real: si se supera el límite, las tareas adicionales se rechazan de forma controlada en lugar
   * de encolarse sin fin. Los hilos virtuales por sí solos no protegen al upstream; este límite sí.
   */
  @Bean
  public Semaphore upstreamConcurrencyLimiter(
      @Value("${rest-clients.product.max-concurrent-calls:50}") final int maxConcurrentCalls) {
    return new Semaphore(maxConcurrentCalls);
  }
}
