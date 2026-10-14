package com.athlete.atlas.port.input.youtube;

public interface RefreshYouTubeVideosUseCase {

  /**
   * Refreshes the video selection of every configured athlete. A failure of one athlete does not
   * stop the others, and never discards a previous selection.
   */
  void refreshAll();
}
