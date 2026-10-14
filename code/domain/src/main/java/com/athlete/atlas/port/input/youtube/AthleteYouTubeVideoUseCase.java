package com.athlete.atlas.port.input.youtube;

import com.athlete.atlas.model.youtube.AthleteYouTubeVideos;
import jakarta.annotation.Nonnull;

public interface AthleteYouTubeVideoUseCase {

  /**
   * Returns the stored video selection of the athlete; it does not call YouTube.
   *
   * @throws com.athlete.atlas.exception.NotFoundException if the athlete has no channel.
   * @throws com.athlete.atlas.exception.DataUnavailableException if no refresh has succeeded yet.
   */
  AthleteYouTubeVideos listAthleteYouTubeVideos(@Nonnull String athleteId);
}
