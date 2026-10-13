package com.athlete.atlas.service;

import java.util.List;

/** Provider-neutral page metadata before any local filtering. */
public record RemotePage<T>(List<T> items, Integer total, Integer totalPages) {}
