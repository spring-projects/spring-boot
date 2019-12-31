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

package org.springframework.boot.autoconfigure.web.embedded;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties.Jetty;
import org.springframework.boot.web.embedded.jetty.JettyThreadPoolFactory;

/**
 * A {@link JettyThreadPoolFactory} that creates a thread pool that uses a backing queue
 * with a max capacity if the {@link Jetty#maxQueueCapacity} is specified. If the max
 * capacity is not specified then the factory will return null thus allowing the standard
 * Jetty server thread pool to be used.
 *
 * @author Chris Bono
 * @since 2.3.0
 */
public class JettyConstrainedQueuedThreadPoolFactory implements JettyThreadPoolFactory<QueuedThreadPool> {

	private ServerProperties serverProperties;

	public JettyConstrainedQueuedThreadPoolFactory(ServerProperties serverProperties) {
		this.serverProperties = serverProperties;
	}

	/**
	 * <p>
	 * Creates a {@link QueuedThreadPool} with the following settings (if
	 * {@link Jetty#maxQueueCapacity} is specified):
	 * <ul>
	 * <li>min threads set to {@link Jetty#minThreads} or {@code 8} if not specified.
	 * <li>max threads set to {@link Jetty#maxThreads} or {@code 200} if not specified.
	 * <li>idle timeout set to {@link Jetty#threadIdleTimeout} or {@code 60000} if not
	 * specified.</li>
	 * <li>if {@link Jetty#maxQueueCapacity} is zero its backing queue will be a
	 * {@link SynchronousQueue} otherwise it will be a {@link BlockingArrayQueue} whose
	 * max capacity is set to the max queue depth.
	 * </ul>
	 * @return thread pool as described above or {@code null} if
	 * {@link Jetty#maxQueueCapacity} is not specified.
	 */
	@Override
	public QueuedThreadPool create() {

		Integer maxQueueCapacity = this.serverProperties.getJetty().getMaxQueueCapacity();

		// Max depth is not specified - let Jetty server use its defaults in this case
		if (maxQueueCapacity == null) {
			return null;
		}

		BlockingQueue<Runnable> queue;
		if (maxQueueCapacity == 0) {
			/**
			 * This queue will cause jetty to reject requests whenever there is no idle
			 * thread available to handle them. If this queue is used, it is strongly
			 * recommended to set _minThreads equal to _maxThreads. Jetty's
			 * QueuedThreadPool class may not behave like a regular java thread pool and
			 * may not add threads properly when a SynchronousQueue is used.
			 */
			queue = new SynchronousQueue<>();
		}
		else {
			/**
			 * Create a queue of fixed size. This queue will not grow. If a request
			 * arrives and the queue is empty, the client will see an immediate
			 * "connection reset" error.
			 */
			queue = new BlockingArrayQueue<>(maxQueueCapacity);
		}

		Integer maxThreadCount = this.serverProperties.getJetty().getMaxThreads();
		Integer minThreadCount = this.serverProperties.getJetty().getMinThreads();
		Duration threadIdleTimeout = this.serverProperties.getJetty().getThreadIdleTimeout();

		return new QueuedThreadPool((maxThreadCount != null) ? maxThreadCount : 200,
				(minThreadCount != null) ? minThreadCount : 8,
				(threadIdleTimeout != null) ? (int) threadIdleTimeout.toMillis() : 60000, queue);
	}

}
