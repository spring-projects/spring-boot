/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.util.Assert;

/**
 * A {@link HealthIndicatorRunner} implementation that uses {@link AsyncTaskExecutor} to
 * invoke {@link HealthIndicator} instances.
 *
 * @author Vedran Pavic
 * @since 2.1.0
 */
public class AsyncHealthIndicatorRunner implements HealthIndicatorRunner {

	private static final Log logger = LogFactory.getLog(AsyncHealthIndicatorRunner.class);

	private final AsyncTaskExecutor taskExecutor;

	/**
	 * Create an {@link AsyncHealthIndicatorRunner} instance.
	 * @param taskExecutor task executor used to run {@link HealthIndicator}s
	 */
	public AsyncHealthIndicatorRunner(AsyncTaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "TaskExecutor must not be null");
		this.taskExecutor = taskExecutor;
	}

	@Override
	public Map<String, Health> run(Map<String, HealthIndicator> healthIndicators) {
		Map<String, Health> healths = new HashMap<>(healthIndicators.size());
		Map<String, Future<Health>> futures = new HashMap<>();
		for (final Map.Entry<String, HealthIndicator> entry : healthIndicators
				.entrySet()) {
			Future<Health> future = this.taskExecutor
					.submit(() -> entry.getValue().health());
			futures.put(entry.getKey(), future);
		}
		for (Map.Entry<String, Future<Health>> entry : futures.entrySet()) {
			try {
				healths.put(entry.getKey(), entry.getValue().get());
			}
			catch (Exception e) {
				logger.warn("Error invoking health indicator '" + entry.getKey() + "'",
						e);
				healths.put(entry.getKey(), Health.down(e).build());
			}
		}
		return healths;
	}

}
