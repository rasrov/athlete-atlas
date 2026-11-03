package com.athlete.atlas.output.rest.ifbb.config;

import com.athlete.atlas.output.rest.ifbb.generated.model.Category;
import com.athlete.atlas.output.rest.ifbb.generated.model.Event;
import com.athlete.atlas.output.rest.ifbb.generated.model.EventImage;
import com.athlete.atlas.output.rest.ifbb.generated.model.EventPage;
import com.athlete.atlas.output.rest.ifbb.generated.model.EventVenue;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.List;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/** Compatibility for the models produced by the configured OpenAPI generator. */
public final class IfbbJsonConfiguration {
  private IfbbJsonConfiguration() {}

  @JsonFormat(shape = JsonFormat.Shape.OBJECT)
  private abstract static class ObjectShape {}

  /**
   * Replaces the Jackson converters with copies using an IFBB-specific mapper. The original
   * converters may be shared with the rest of the application, so they are never modified.
   */
  public static void configure(final List<HttpMessageConverter<?>> converters) {
    converters.replaceAll(
        converter ->
            converter instanceof JacksonJsonHttpMessageConverter json
                ? new JacksonJsonHttpMessageConverter(ifbbMapper(json.getMapper()))
                : converter);
  }

  /** Jackson 3 mappers are immutable: the IFBB mapper is a rebuilt copy of the base one. */
  private static JsonMapper ifbbMapper(final JsonMapper base) {
    var module = new SimpleModule();
    module.addDeserializer(EventVenue.class, new ValueDeserializer<>() {
      @Override public EventVenue deserialize(JsonParser parser, DeserializationContext context) {
        JsonNode node = context.readTree(parser);
        if (node.isNull() || node.isBoolean() && !node.booleanValue() || node.isArray() && node.isEmpty()) return null;
        if (!node.isObject()) return (EventVenue) context.handleUnexpectedToken(EventVenue.class, parser);
        return new EventVenue().city(text(node, "city", context)).country(text(node, "country", context));
      }
    });
    module.addDeserializer(EventImage.class, new ValueDeserializer<>() {
      @Override public EventImage deserialize(JsonParser parser, DeserializationContext context) {
        JsonNode node = context.readTree(parser);
        if (node.isNull() || node.isBoolean() && !node.booleanValue()) return null;
        if (!node.isObject()) return (EventImage) context.handleUnexpectedToken(EventImage.class, parser);
        return new EventImage().url(text(node, "url", context));
      }
    });
    return base.rebuild()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .addMixIn(EventPage.class, ObjectShape.class)
        .addMixIn(Event.class, ObjectShape.class)
        .addMixIn(Category.class, ObjectShape.class)
        .addModule(module)
        .build();
  }

  private static String text(JsonNode object, String field, DeserializationContext context) {
    var value = object.get(field);
    if (value == null || value.isNull()) return null;
    if (!value.isString()) return context.reportInputMismatch(String.class, "Invalid IFBB text field: %s", field);
    return value.stringValue();
  }
}
