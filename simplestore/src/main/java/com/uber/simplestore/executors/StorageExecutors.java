/*
 * Copyright (C) 2019. Uber Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.uber.simplestore.executors;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Like MoreExecutors, but no Guava. */
public final class StorageExecutors {

  private static final Executor MAIN_EXECUTOR = new MainThreadExecutor();

  private static int ioThreadCount = 0;
  private static final Executor IO_EXECUTOR =
      Executors.newCachedThreadPool(r -> new Thread(r, "SimpleStoreIO-" + ioThreadCount++));

  private static int compThreadCount = 0;
  private static final Executor COMPUTATION_EXECUTOR =
      Executors.newFixedThreadPool(2, r -> new Thread(r, "SimpleStoreComp-" + compThreadCount++));

  public static Executor mainExecutor() {
    return MAIN_EXECUTOR;
  }

  public static Executor computationExecutor() {
    return COMPUTATION_EXECUTOR;
  }

  public static Executor ioExecutor() {
    return IO_EXECUTOR;
  }

  static class MainThreadExecutor implements Executor {
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void execute(Runnable r) {
      handler.post(r);
    }
  }
}
