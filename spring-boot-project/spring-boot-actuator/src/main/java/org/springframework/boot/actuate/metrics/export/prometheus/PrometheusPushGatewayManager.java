/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.metrics.export.prometheus;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;

/**
 * Class that can be used to manage the pushing of metrics to a {@link PushGateway
 * Prometheus PushGateway}. Handles the scheduling of push operations, error handling and
 * shutdown operations.
 *
 * @author David J. M. Karlsen
 * @author Phillip Webb
 * @since 2.1.0
 */
public class PrometheusPushGatewayManager {

	private static final Log logger = LogFactory.getLog(PrometheusPushGatewayManager.class);

	private final PushGateway pushGateway;

	private final ShutdownOperation shutdownOperation;

	private final TaskScheduler scheduler;

	private final ScheduledFuture<?> scheduled;

	/**
	 * Create a new {@link PrometheusPushGatewayManager} instance.
	 * @param pushGateway the source push gateway
	 * @param pushRate the rate at which push operations occur
	 * @param shutdownOperation the shutdown operation that should be performed when
	 * context is closed.
	 * @since 3.5.0
	 */
	public PrometheusPushGatewayManager(PushGateway pushGateway, Duration pushRate,
			ShutdownOperation shutdownOperation) {
		this(pushGateway, new PushGatewayTaskScheduler(), pushRate, shutdownOperation);
	}

	PrometheusPushGatewayManager(PushGateway pushGateway, TaskScheduler scheduler, Duration pushRate,
			ShutdownOperation shutdownOperation) {
		Assert.notNull(pushGateway, "'pushGateway' must not be null");
		Assert.notNull(scheduler, "'scheduler' must not be null");
		Assert.notNull(pushRate, "'pushRate' must not be null");
		this.pushGateway = pushGateway;
		this.shutdownOperation = (shutdownOperation != null) ? shutdownOperation : ShutdownOperation.NONE;
		this.scheduler = scheduler;
		this.scheduled = this.scheduler.scheduleAtFixedRate(this::post, pushRate);
	}

	private void post() {
		try {
			this.pushGateway.pushAdd();
		}
		catch (Throwable ex) {
			logger.warn("Unexpected exception thrown by POST of metrics to Prometheus Pushgateway", ex);
		}
	}

	private void put() {
		try {
			this.pushGateway.push();
		}
		catch (Throwable ex) {
			logger.warn("Unexpected exception thrown by PUT of metrics to Prometheus Pushgateway", ex);
		}
	}

	private void delete() {
		try {
			this.pushGateway.delete();
		}
		catch (Throwable ex) {
			logger.warn("Unexpected exception thrown by DELETE of metrics from Prometheus Pushgateway", ex);
		}
	}

	/**
	 * Shutdown the manager, running any {@link ShutdownOperation}.
	 */
	public void shutdown() {
		shutdown(this.shutdownOperation);
	}

	private void shutdown(ShutdownOperation shutdownOperation) {
		if (this.scheduler instanceof PushGatewayTaskScheduler pushGatewayTaskScheduler) {
			pushGatewayTaskScheduler.shutdown();
		}
		this.scheduled.cancel(false);
		switch (shutdownOperation) {
			case POST -> post();
			case PUT -> put();
			case DELETE -> delete();
		}
	}

	/**
	 * The operation that should be performed on shutdown.
	 */
	public enum ShutdownOperation {

		/**
		 * Don't perform any shutdown operation.
		 */
		NONE,

		/**
		 * Perform a POST before shutdown.
		 */
		POST,

		/**
		 * Perform a PUT before shutdown.
		 */
		PUT,

		/**
		 * Perform a DELETE before shutdown.
		 */
		DELETE

	}

	/**
	 * {@link TaskScheduler} used when the user doesn't specify one.
	 */
	static class PushGatewayTaskScheduler extends ThreadPoolTaskScheduler {

		PushGatewayTaskScheduler() {
			setPoolSize(1);
			setDaemon(true);
			setThreadGroupName("prometheus-push-gateway");
		}

		@Override
		public ScheduledExecutorService getScheduledExecutor() throws IllegalStateException {
			return Executors.newSingleThreadScheduledExecutor(this::newThread);
		}

	}

}
