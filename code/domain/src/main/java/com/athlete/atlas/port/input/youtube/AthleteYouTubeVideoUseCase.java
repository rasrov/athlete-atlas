package com.athlete.atlas.port.input.youtube;

import com.athlete.atlas.model.selection.AthleteSelection;
import com.athlete.atlas.model.youtube.YouTubeVideo;
import jakarta.annotation.Nonnull;

public interface AthleteYouTubeVideoUseCase {

  /**
   * Returns the stored video selection of the athlete; it does not call YouTube.
   *
   * @throws com.athlete.atlas.exception.NotFoundException if the athlete does not exist or has no
   *     channel.
   * @throws com.athlete.atlas.exception.DataUnavailableException if no refresh has succeeded yet.
   * @throws com.athlete.atlas.exception.StorageUnavailableException if the athletes cannot be read.
   */
  AthleteSelection<YouTubeVideo> listAthleteYouTubeVideos(@Nonnull String athleteId);
}
