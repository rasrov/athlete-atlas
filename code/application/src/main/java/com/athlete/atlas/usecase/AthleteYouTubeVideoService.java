package com.athlete.atlas.usecase;

import com.athlete.atlas.config.YouTubeSelectionProperties;
import com.athlete.atlas.exception.DataUnavailableException;
import com.athlete.atlas.exception.NotFoundException;
import com.athlete.atlas.exception.RemoteException;
import com.athlete.atlas.model.youtube.AthleteYouTubeVideos;
import com.athlete.atlas.model.youtube.YouTubeVideoSelection;
import com.athlete.atlas.port.input.youtube.AthleteYouTubeVideoUseCase;
import com.athlete.atlas.port.input.youtube.RefreshYouTubeVideosUseCase;
import com.athlete.atlas.port.output.YouTubePort;
import com.athlete.atlas.port.output.YouTubeVideoSelectionRepository;
import jakarta.annotation.Nonnull;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Serves the stored video selection of each athlete and refreshes it from YouTube. The latest
 * successful refresh always wins, whatever the number of videos (fewer than the selection size,
 * even none, is valid data). A failed refresh keeps the previous selection and marks it stale.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AthleteYouTubeVideoService
    implements AthleteYouTubeVideoUseCase, RefreshYouTubeVideosUseCase {

  private final YouTubePort youTubePort;

  private final YouTubeVideoSelectionRepository youTubeVideoSelectionRepository;

  private final YouTubeSelectionProperties youTubeSelectionProperties;

  private final Clock clock;

  @Override
  public AthleteYouTubeVideos listAthleteYouTubeVideos(@Nonnull final String athleteId) {
    if (!youTubeSelectionProperties.channels().containsKey(athleteId)) {
      throw new NotFoundException("Athlete without YouTube channel: " + athleteId);
    }
    final var selection =
        youTubeVideoSelectionRepository
            .findByAthleteId(athleteId)
            .orElseThrow(
                () ->
                    new DataUnavailableException(
                        "YouTube videos not synchronized yet for athlete " + athleteId));
    return new AthleteYouTubeVideos(
        athleteId,
        selection.videos(),
        selection.updatedAt(),
        selection.availability(clock.instant(), youTubeSelectionProperties.freshnessThreshold()));
  }

  @Override
  public void refreshAll() {
    youTubeSelectionProperties.channels().forEach(this::refresh);
  }

  private void refresh(final String athleteId, final String channelId) {
    try {
      final var videos = youTubePort.fetchLatestVideos(channelId);
      youTubeVideoSelectionRepository.save(
          athleteId, YouTubeVideoSelection.refreshedAt(videos, clock.instant()));
      log.info("YouTube selection of athlete {} refreshed: {} videos", athleteId, videos.size());
    } catch (final RuntimeException e) {
      youTubeVideoSelectionRepository
          .findByAthleteId(athleteId)
          .ifPresent(
              previous ->
                  youTubeVideoSelectionRepository.save(athleteId, previous.withFailedRefresh()));
      if (e instanceof RemoteException) {
        // The provider error is already logged where it is translated.
        log.warn(
            "YouTube refresh of athlete {} failed, keeping the previous selection: {}",
            athleteId,
            e.getMessage());
      } else {
        log.error(
            "Unexpected error refreshing the YouTube selection of athlete {}, keeping the previous"
                + " selection",
            athleteId,
            e);
      }
    }
  }
}
