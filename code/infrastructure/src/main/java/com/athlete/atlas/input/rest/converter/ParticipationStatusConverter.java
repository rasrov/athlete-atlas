package com.athlete.atlas.input.rest.converter;

import com.athlete.atlas.infrastructure.api.model.ParticipationStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts the query parameter using the contract value ({@code confirmed}) instead of the enum
 * constant name ({@code CONFIRMED}), which is what Spring MVC uses by default. The generator only
 * creates this converter when it also generates the controllers (interfaceOnly=false). An unknown
 * value throws IllegalArgumentException, which Spring MVC reports as a 400.
 */
@Component
public class ParticipationStatusConverter implements Converter<String, ParticipationStatus> {

  @Override
  public ParticipationStatus convert(final String source) {
    return ParticipationStatus.fromValue(source);
  }
}
