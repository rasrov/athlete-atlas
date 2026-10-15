package com.athlete.atlas.usecase;

import com.athlete.atlas.config.YouTubeSelectionProperties;
import com.athlete.atlas.model.athlete.Athlete;
import com.athlete.atlas.model.selection.AthleteSelection;
import com.athlete.atlas.model.youtube.YouTubeVideo;
import com.athlete.atlas.port.input.youtube.AthleteYouTubeVideoUseCase;
import com.athlete.atlas.port.input.youtube.RefreshYouTubeVideosUseCase;
import com.athlete.atlas.port.output.AthleteRepository;
import com.athlete.atlas.port.output.YouTubePort;
import com.athlete.atlas.port.output.YouTubeVideoSelectionRepository;
import com.athlete.atlas.usecase.selection.SynchronizedSelectionService;
import jakarta.annotation.Nonnull;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/** Latest YouTube videos of each athlete, refreshed periodically from their channel. */
@Component
public class AthleteYouTubeVideoService extends SynchronizedSelectionService<YouTubeVideo>
    implements AthleteYouTubeVideoUseCase, RefreshYouTubeVideosUseCase {

  private final YouTubePort youTubePort;

  private final YouTubeSelectionProperties youTubeSelectionProperties;

  public AthleteYouTubeVideoService(
      final YouTubePort youTubePort,
      final YouTubeVideoSelectionRepository youTubeVideoSelectionRepository,
      final AthleteRepository athleteRepository,
      final YouTubeSelectionProperties youTubeSelectionProperties,
      final Clock clock) {
    super("YouTube", youTubeVideoSelectionRepository, athleteRepository, clock);
    this.youTubePort = youTubePort;
    this.youTubeSelectionProperties = youTubeSelectionProperties;
  }

  @Override
  public AthleteSelection<YouTubeVideo> listAthleteYouTubeVideos(@Nonnull final String athleteId) {
    return getSelection(athleteId);
  }

  @Override
  protected String account(final Athlete athlete) {
    return athlete.youTubeChannelId();
  }

  @Override
  protected Duration freshnessThreshold() {
    return youTubeSelectionProperties.freshnessThreshold();
  }

  @Override
  protected List<YouTubeVideo> fetch(final String channelId) {
    return youTubePort.fetchLatestVideos(channelId);
  }
}
