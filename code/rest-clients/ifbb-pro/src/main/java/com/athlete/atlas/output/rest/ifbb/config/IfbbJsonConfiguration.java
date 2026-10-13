package com.athlete.atlas.output.rest.ifbb.config;

import com.athlete.atlas.output.rest.ifbb.generated.model.Category;
import com.athlete.atlas.output.rest.ifbb.generated.model.Event;
import com.athlete.atlas.output.rest.ifbb.generated.model.EventImage;
import com.athlete.atlas.output.rest.ifbb.generated.model.EventPage;
import com.athlete.atlas.output.rest.ifbb.generated.model.EventVenue;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.List;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

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
            converter instanceof MappingJackson2HttpMessageConverter json
                ? new MappingJackson2HttpMessageConverter(ifbbMapper(json.getObjectMapper()))
                : converter);
  }

  private static ObjectMapper ifbbMapper(final ObjectMapper base) {
    var mapper = base.copy();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.addMixIn(EventPage.class, ObjectShape.class);
    mapper.addMixIn(Event.class, ObjectShape.class);
    mapper.addMixIn(Category.class, ObjectShape.class);
    var module = new SimpleModule();
    module.addDeserializer(EventVenue.class, new JsonDeserializer<>() {
      @Override public EventVenue deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (node.isNull() || node.isBoolean() && !node.booleanValue() || node.isArray() && node.isEmpty()) return null;
        if (!node.isObject()) return (EventVenue) context.handleUnexpectedToken(EventVenue.class, parser);
        return new EventVenue().city(text(node, "city")).country(text(node, "country"));
      }
    });
    module.addDeserializer(EventImage.class, new JsonDeserializer<>() {
      @Override public EventImage deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (node.isNull() || node.isBoolean() && !node.booleanValue()) return null;
        if (!node.isObject()) return (EventImage) context.handleUnexpectedToken(EventImage.class, parser);
        return new EventImage().url(text(node, "url"));
      }
    });
    mapper.registerModule(module);
    return mapper;
  }

  private static String text(JsonNode object, String field) throws IOException {
    var value = object.get(field);
    if (value == null || value.isNull()) return null;
    if (!value.isTextual()) throw new IOException("Invalid IFBB text field: " + field);
    return value.textValue();
  }
}
