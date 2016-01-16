/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.health;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Default implementation of {@link HealthIndicatorRegistry}.
 *
 * @author Vedran Pavic
 */
public class DefaultHealthIndicatorRegistry implements HealthIndicatorRegistry {

	private static final Log logger = LogFactory.getLog(DefaultHealthIndicatorRegistry.class);

	private final Map<String, HealthIndicator> healthIndicators =
			new HashMap<String, HealthIndicator>();

	private DefaultHealthIndicatorRegistryProperties properties;

	private ExecutorService executor;

	/**
	 * Create a {@link DefaultHealthIndicatorRegistry} instance.
	 * @param properties the configuration properties
	 */
	public DefaultHealthIndicatorRegistry(DefaultHealthIndicatorRegistryProperties properties) {
		Assert.notNull(properties, "Properties must not be null");
		this.properties = properties;
		if (this.properties.isRunInParallel()) {
			this.executor = Executors.newFixedThreadPool(
					this.properties.getThreadCount(), new WorkerThreadFactory());
		}
	}

	/**
	 * Create a {@link DefaultHealthIndicatorRegistry} instance.
	 */
	public DefaultHealthIndicatorRegistry() {
		this(new DefaultHealthIndicatorRegistryProperties());
	}

	@Override
	public void register(String name, HealthIndicator healthIndicator) {
		synchronized (this.healthIndicators) {
			this.healthIndicators.put(name, healthIndicator);
		}
	}

	@Override
	public void unregister(String name) {
		synchronized (this.healthIndicators) {
			this.healthIndicators.remove(name);
		}
	}

	@Override
	public Set<String> getRegisteredNames() {
		return Collections.unmodifiableSet(new HashSet<String>(this.healthIndicators.keySet()));
	}

	@Override
	public Health runHealthIndicator(String name) {
		HealthIndicator healthIndicator = this.healthIndicators.get(name);
		Assert.notNull(healthIndicator, "HealthIndicator " + name + " does not exist");
		return healthIndicator.health();
	}

	@Override
	public Map<String, Health> runHealthIndicators() {
		Map<String, Health> healths = new HashMap<String, Health>();
		if (this.properties.isRunInParallel()) {
			Assert.notNull(this.executor, "Executor must not be null");
			Map<String, Future<Health>> futures = new HashMap<String, Future<Health>>();
			for (final Map.Entry<String, HealthIndicator> entry : this.healthIndicators.entrySet()) {
				Future<Health> future = this.executor.submit(new Callable<Health>() {
					@Override
					public Health call() throws Exception {
						return entry.getValue().health();
					}
				});
				futures.put(entry.getKey(), future);
			}
			for (Map.Entry<String, Future<Health>> entry : futures.entrySet()) {
				try {
					healths.put(entry.getKey(), entry.getValue().get());
				}
				catch (Exception e) {
					logger.warn("Error invoking health indicator '" + entry.getKey() + "'", e);
					healths.put(entry.getKey(), Health.down(e).build());
				}
			}
		}
		else {
			for (Map.Entry<String, HealthIndicator> entry : this.healthIndicators.entrySet()) {
				healths.put(entry.getKey(), entry.getValue().health());
			}
		}
		return healths;
	}

	/**
	 * {@link ThreadFactory} to create the worker threads.
	 */
	private static class WorkerThreadFactory implements ThreadFactory {

		private final AtomicInteger threadNumber = new AtomicInteger(1);

		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setName(ClassUtils.getShortName(getClass()) +
					"-" + this.threadNumber.getAndIncrement());
			return thread;
		}

	}

}
