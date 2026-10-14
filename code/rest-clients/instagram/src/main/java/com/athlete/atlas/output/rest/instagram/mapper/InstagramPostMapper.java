package com.athlete.atlas.output.rest.instagram.mapper;

import com.athlete.atlas.model.instagram.InstagramMediaType;
import com.athlete.atlas.model.instagram.InstagramPost;
import com.athlete.atlas.output.rest.instagram.generated.model.Media;
import java.net.URI;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * Maps provider media to the domain. It only transforms posts already accepted by the selection
 * policy: it makes no HTTP calls and decides nothing about which posts are kept.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InstagramPostMapper {

  @Mapping(target = "id", source = "id")
  @Mapping(target = "mediaType", source = "mediaType", qualifiedByName = "mediaType")
  @Mapping(target = "publishedAt", source = "timestamp", qualifiedByName = "publishedAt")
  @Mapping(target = "permalink", source = "permalink", qualifiedByName = "permalink")
  @Mapping(target = "previewImageUrl", source = ".", qualifiedByName = "previewImageUrl")
  InstagramPost toDomain(Media media);

  @Named("mediaType")
  default InstagramMediaType mediaType(final String value) {
    return InstagramPostValues.mediaType(value)
        .orElseThrow(() -> new IllegalArgumentException("Unsupported media type"));
  }

  @Named("publishedAt")
  default Instant publishedAt(final String value) {
    return InstagramPostValues.publishedAt(value)
        .orElseThrow(() -> new IllegalArgumentException("Invalid timestamp"));
  }

  @Named("permalink")
  default URI permalink(final String value) {
    return InstagramPostValues.permalink(value)
        .orElseThrow(() -> new IllegalArgumentException("Invalid permalink"));
  }

  /** Validated cover of the post, or null: a post without cover is still shown by its link. */
  @Named("previewImageUrl")
  default URI previewImageUrl(final Media media) {
    return InstagramPostValues.mediaType(media.getMediaType())
        .flatMap(
            type ->
                InstagramPostValues.previewImageUrl(
                    type, media.getMediaUrl(), media.getThumbnailUrl()))
        .orElse(null);
  }
}
