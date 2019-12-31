/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.web.embedded.jetty;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.ThreadPool;

/**
 * Factory to create a thread pool for use by a Jetty {@link Server}.
 *
 * @author Chris Bono
 * @since 2.3.0
 * @param <T> type of ThreadPool that the factory returns
 */
@FunctionalInterface
public interface JettyThreadPoolFactory<T extends ThreadPool> {

	/**
	 * Creates a thread pool.
	 * @return a Jetty thread pool or null to use the default Jetty {@link Server} thread
	 * pool.
	 */
	T create();

}
