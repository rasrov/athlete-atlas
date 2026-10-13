package com.athlete.atlas.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfiguration {

  /** Injected instead of calling LocalDate.now() directly, so that tests can fix the date. */
  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
