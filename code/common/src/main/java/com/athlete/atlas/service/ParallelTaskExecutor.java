package com.athlete.atlas.service;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public interface ParallelTaskExecutor {

  <T, R> List<TaskResult<R>> executeInParallel(Collection<T> input, Function<T, R> task);
}
