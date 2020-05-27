/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.rsocket.server;

import io.rsocket.RSocketFactory.ServerRSocketFactory;

/**
 * Processor that allows for custom modification of a {@link ServerRSocketFactory
 * RSocketFactory.ServerRSocketFactory} before it is used.
 *
 * @author Brian Clozel
 * @see RSocketServerFactory
 * @since 2.2.0
 * @deprecated in favor of {@link RSocketServerCustomizer} as of 2.2.7
 */
@FunctionalInterface
@Deprecated
public interface ServerRSocketFactoryProcessor {

	/**
	 * Apply this {@code ServerRSocketFactoryProcessor} to the given factory instance
	 * before it's used.
	 * @param factory the factory to process
	 * @return the processed factory instance
	 */
	ServerRSocketFactory process(ServerRSocketFactory factory);

}
