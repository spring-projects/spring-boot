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

import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JettyConstrainedQueuedThreadPoolFactory}.
 *
 * @author Chris Bono
 */
class JettyConstrainedQueuedThreadPoolFactoryTest {

	private MockEnvironment environment;

	private ServerProperties serverProperties;

	private JettyConstrainedQueuedThreadPoolFactory factory;

	@BeforeEach
	void setup() {
		this.environment = new MockEnvironment();
		this.serverProperties = new ServerProperties();
		ConfigurationPropertySources.attach(this.environment);
		this.factory = new JettyConstrainedQueuedThreadPoolFactory(this.serverProperties);
	}

	@Test
	void factoryReturnsNullWhenMaxCapacityNotSpecified() {
		bind("server.jetty.max-queue-capacity=");
		assertThat(this.factory.create()).isNull();
	}

	@Test
	void factoryReturnsSynchronousQueueWhenMaxCapacityIsZero() {
		bind("server.jetty.max-queue-capacity=0");
		QueuedThreadPool queuedThreadPool = this.factory.create();
		assertThat(getQueue(queuedThreadPool, SynchronousQueue.class)).isNotNull();
	}

	@Test
	void factoryReturnsBlockingArrayQueueWithDefaultsWhenOnlyMaxCapacityIsSet() {
		bind("server.jetty.max-queue-capacity=5150");
		QueuedThreadPool queuedThreadPool = this.factory.create();
		assertThat(queuedThreadPool.getMinThreads()).isEqualTo(8);
		assertThat(queuedThreadPool.getMaxThreads()).isEqualTo(200);
		assertThat(queuedThreadPool.getIdleTimeout()).isEqualTo(60000);
		assertThat(getQueue(queuedThreadPool, BlockingArrayQueue.class).getMaxCapacity()).isEqualTo(5150);
	}

	@Test
	void factoryReturnsBlockingArrayQueueWithCustomValues() {
		bind("server.jetty.max-queue-capacity=5150", "server.jetty.min-threads=200", "server.jetty.max-threads=1000",
				"server.jetty.thread-idle-timeout=10000");
		QueuedThreadPool queuedThreadPool = this.factory.create();
		assertThat(queuedThreadPool.getMinThreads()).isEqualTo(200);
		assertThat(queuedThreadPool.getMaxThreads()).isEqualTo(1000);
		assertThat(queuedThreadPool.getIdleTimeout()).isEqualTo(10000);
		assertThat(getQueue(queuedThreadPool, BlockingArrayQueue.class).getMaxCapacity()).isEqualTo(5150);
	}

	private void bind(String... inlinedProperties) {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, inlinedProperties);
		new Binder(ConfigurationPropertySources.get(this.environment)).bind("server",
				Bindable.ofInstance(this.serverProperties));
	}

	static <T extends BlockingQueue<Runnable>> T getQueue(QueuedThreadPool queuedThreadPool,
			Class<T> expectedQueueClass) {
		Method getQueue = ReflectionUtils.findMethod(QueuedThreadPool.class, "getQueue");
		ReflectionUtils.makeAccessible(getQueue);
		Object obj = ReflectionUtils.invokeMethod(getQueue, queuedThreadPool);
		assertThat(obj).isInstanceOf(expectedQueueClass);
		return expectedQueueClass.cast(obj);
	}

}
