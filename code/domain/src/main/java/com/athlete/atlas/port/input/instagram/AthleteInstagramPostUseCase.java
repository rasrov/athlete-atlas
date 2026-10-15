package com.athlete.atlas.port.input.instagram;

import com.athlete.atlas.model.instagram.InstagramPost;
import com.athlete.atlas.model.selection.AthleteSelection;
import jakarta.annotation.Nonnull;

public interface AthleteInstagramPostUseCase {

  /**
   * Returns the stored post selection of the athlete; it does not call Instagram.
   *
   * @throws com.athlete.atlas.exception.NotFoundException if the athlete does not exist or has no
   *     Instagram profile.
   * @throws com.athlete.atlas.exception.DataUnavailableException if no refresh has succeeded yet.
   * @throws com.athlete.atlas.exception.StorageUnavailableException if the athletes cannot be read.
   */
  AthleteSelection<InstagramPost> listAthleteInstagramPosts(@Nonnull String athleteId);
}
