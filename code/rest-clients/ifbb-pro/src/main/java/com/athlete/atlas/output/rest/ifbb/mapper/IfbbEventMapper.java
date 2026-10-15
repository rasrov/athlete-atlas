package com.athlete.atlas.output.rest.ifbb.mapper;

import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.output.rest.ifbb.generated.model.Event;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.web.util.HtmlUtils;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IfbbEventMapper {

  @Mapping(target = "id", source = "id", qualifiedByName = "externalId")
  @Mapping(target = "sourceId", source = "id")
  @Mapping(target = "name", source = "title", qualifiedByName = "plainText")
  @Mapping(target = "description", source = "description", qualifiedByName = "plainText")
  @Mapping(target = "startDate", source = "startDate", qualifiedByName = "localDate")
  @Mapping(target = "endDate", source = "endDate", qualifiedByName = "localDate")
  @Mapping(target = "city", source = "venue.city", qualifiedByName = "optionalPlainText")
  @Mapping(target = "country", source = "venue.country", qualifiedByName = "optionalPlainText")
  @Mapping(target = "sourceUrl", source = "url")
  @Mapping(target = "logoUrl", ignore = true)
  AthleteCompetition toDomain(Event event);

  @Named("externalId")
  default String externalId(Long value) {
    return "ifbb-" + value;
  }

  @Named("localDate")
  default LocalDate localDate(String value) {
    if (value == null) return null;
    return LocalDateTime.parse(
            value,
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
                .withResolverStyle(ResolverStyle.STRICT))
        .toLocalDate();
  }

  /** Plain text for nullable fields: missing or blank values become null. */
  @Named("optionalPlainText")
  default String optionalPlainText(String value) {
    final String text = plainText(value);
    return text == null || text.isEmpty() ? null : text;
  }

  @Named("plainText")
  default String plainText(String value) {
    if (value == null) return null;
    return HtmlUtils.htmlUnescape(value.replaceAll("<[^>]*>", " "))
        .replaceAll("[\\s\\u00a0]+", " ")
        .trim();
  }
}
