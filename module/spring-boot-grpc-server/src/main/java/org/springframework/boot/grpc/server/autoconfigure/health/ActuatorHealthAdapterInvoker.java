/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.server.autoconfigure.health;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.task.SimpleAsyncTaskSchedulerBuilder;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

/**
 * Periodically invokes the {@link ActuatorHealthAdapter} in the background.
 *
 * @author Chris Bono
 */
class ActuatorHealthAdapterInvoker implements InitializingBean, DisposableBean {

	private final ActuatorHealthAdapter healthAdapter;

	private final SimpleAsyncTaskScheduler taskScheduler;

	private final Duration updateInitialDelay;

	private final Duration updateFixedRate;

	ActuatorHealthAdapterInvoker(ActuatorHealthAdapter healthAdapter, SimpleAsyncTaskSchedulerBuilder schedulerBuilder,
			Duration updateInitialDelay, Duration updateFixedRate) {
		this.healthAdapter = healthAdapter;
		this.taskScheduler = schedulerBuilder.threadNamePrefix("healthAdapter-").build();
		this.updateInitialDelay = updateInitialDelay;
		this.updateFixedRate = updateFixedRate;
	}

	@Override
	public void afterPropertiesSet() {
		this.taskScheduler.scheduleAtFixedRate(this::updateHealthStatus, Instant.now().plus(this.updateInitialDelay),
				this.updateFixedRate);
	}

	@Override
	public void destroy() {
		this.taskScheduler.close();
	}

	void updateHealthStatus() {
		this.healthAdapter.updateHealthStatus();
	}

}
