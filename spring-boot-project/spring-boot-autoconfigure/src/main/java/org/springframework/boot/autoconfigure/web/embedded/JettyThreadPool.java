/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.embedded;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

import org.springframework.boot.autoconfigure.web.ServerProperties;

/**
 * Creates a {@link ThreadPool} for Jetty, applying
 * {@link org.springframework.boot.autoconfigure.web.ServerProperties.Jetty.Threads
 * ServerProperties.Jetty.Threads Jetty thread properties}.
 *
 * @author Moritz Halbritter
 */
final class JettyThreadPool {

	private JettyThreadPool() {
	}

	static QueuedThreadPool create(ServerProperties.Jetty.Threads properties) {
		BlockingQueue<Runnable> queue = determineBlockingQueue(properties.getMaxQueueCapacity());
		int maxThreadCount = (properties.getMax() > 0) ? properties.getMax() : 200;
		int minThreadCount = (properties.getMin() > 0) ? properties.getMin() : 8;
		int threadIdleTimeout = (properties.getIdleTimeout() != null) ? (int) properties.getIdleTimeout().toMillis()
				: 60000;
		return new QueuedThreadPool(maxThreadCount, minThreadCount, threadIdleTimeout, queue);
	}

	private static BlockingQueue<Runnable> determineBlockingQueue(Integer maxQueueCapacity) {
		if (maxQueueCapacity == null) {
			return null;
		}
		if (maxQueueCapacity == 0) {
			return new SynchronousQueue<>();
		}
		return new BlockingArrayQueue<>(maxQueueCapacity);
	}

}
