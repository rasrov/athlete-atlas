package com.athlete.atlas.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfiguration {

  /**
   * Injected instead of calling LocalDate.now() directly, so that tests can fix the date. Local
   * dates (e.g. the competition season) are computed in the zone of the API contract, whatever the
   * time zone of the server; instants are not affected.
   */
  @Bean
  public Clock clock(@Value("${app.time-zone:Europe/Madrid}") final ZoneId zone) {
    return Clock.system(zone);
  }
}
