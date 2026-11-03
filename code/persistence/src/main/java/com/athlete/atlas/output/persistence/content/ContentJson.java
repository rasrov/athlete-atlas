package com.athlete.atlas.output.persistence.content;

import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON format of the stored items. It has its own ObjectMapper, so that a change of the web
 * configuration of Jackson does not change the stored format: dates as ISO text, unknown fields
 * ignored so that removing a field does not invalidate the stored data. Both are set explicitly,
 * even when they match the Jackson defaults, so that a Jackson upgrade cannot change them.
 */
final class ContentJson {

  private static final ObjectMapper MAPPER =
      JsonMapper.builder()
          .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .build();

  private ContentJson() {}

  static String write(final List<?> items) {
    try {
      return MAPPER.writeValueAsString(items);
    } catch (final JacksonException e) {
      // The stored records only hold strings, dates and URIs: it cannot fail unless there is a bug.
      throw new IllegalStateException("Cannot serialize the stored items", e);
    }
  }

  /**
   * @throws JacksonException if the JSON does not have the expected format.
   */
  static <S> List<S> readList(final String json, final Class<S> type) {
    return MAPPER.readValue(json, MAPPER.getTypeFactory().constructCollectionType(List.class, type));
  }
}
