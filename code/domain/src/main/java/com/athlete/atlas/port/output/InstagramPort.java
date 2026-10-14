package com.athlete.atlas.port.output;

import com.athlete.atlas.model.instagram.InstagramPost;
import jakarta.annotation.Nonnull;
import java.util.List;

public interface InstagramPort {

  /**
   * Fetches the latest valid posts of an Instagram profile.
   *
   * @param username Instagram username, taken from the validated athlete configuration.
   * @return up to the configured selection size of distinct posts, sorted by publication date
   *     descending and then by id; fewer (even none) when the profile does not have more.
   * @throws com.athlete.atlas.exception.RemoteException when the provider fails, rejects the
   *     credentials or returns another profile.
   */
  List<InstagramPost> fetchLatestPosts(@Nonnull String username);
}
