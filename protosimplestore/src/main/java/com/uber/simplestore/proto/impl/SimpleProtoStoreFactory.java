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
package com.uber.simplestore.proto.impl;

import com.uber.simplestore.DirectoryProvider;
import com.uber.simplestore.NamespaceConfig;
import com.uber.simplestore.impl.SimpleStoreFactory;
import com.uber.simplestore.proto.SimpleProtoStore;

/**
 * Obtain an instance of a storage namespace with proto support. Only one instance per namespace may
 * exist at any time.
 *
 * <p>Delegates to {@link SimpleStoreFactory#create(com.uber.simplestore.DirectoryProvider, String,
 * NamespaceConfig)}.
 */
public final class SimpleProtoStoreFactory {

  public static SimpleProtoStore create(DirectoryProvider directoryProvider, String namespace) {
    return create(directoryProvider, namespace, NamespaceConfig.DEFAULT);
  }

  public static SimpleProtoStore create(
      DirectoryProvider directoryProvider, String namespace, NamespaceConfig config) {
    return new SimpleProtoStoreImpl(
        SimpleStoreFactory.create(directoryProvider, namespace, config), config);
  }
}
