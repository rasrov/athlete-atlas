package com.athlete.atlas.port.input.instagram;

public interface RefreshInstagramPostsUseCase {

  /**
   * Refreshes the post selection of every configured athlete. A failure of one athlete does not
   * stop the others, and never discards a previous selection. Also usable for a manual refresh.
   */
  void refreshAll();
}
