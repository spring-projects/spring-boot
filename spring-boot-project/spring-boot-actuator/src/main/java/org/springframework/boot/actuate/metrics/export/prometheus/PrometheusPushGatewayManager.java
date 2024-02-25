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

package org.springframework.boot.actuate.metrics.export.prometheus;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
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

	private final CollectorRegistry registry;

	private final String job;

	private final Map<String, String> groupingKey;

	private final ShutdownOperation shutdownOperation;

	private final TaskScheduler scheduler;

	private final ScheduledFuture<?> scheduled;

	/**
	 * Create a new {@link PrometheusPushGatewayManager} instance using a single threaded
	 * {@link TaskScheduler}.
	 * @param pushGateway the source push gateway
	 * @param registry the collector registry to push
	 * @param pushRate the rate at which push operations occur
	 * @param job the job ID for the operation
	 * @param groupingKeys an optional set of grouping keys for the operation
	 * @param shutdownOperation the shutdown operation that should be performed when
	 * context is closed.
	 */
	public PrometheusPushGatewayManager(PushGateway pushGateway, CollectorRegistry registry, Duration pushRate,
			String job, Map<String, String> groupingKeys, ShutdownOperation shutdownOperation) {
		this(pushGateway, registry, new PushGatewayTaskScheduler(), pushRate, job, groupingKeys, shutdownOperation);
	}

	/**
	 * Create a new {@link PrometheusPushGatewayManager} instance.
	 * @param pushGateway the source push gateway
	 * @param registry the collector registry to push
	 * @param scheduler the scheduler used for operations
	 * @param pushRate the rate at which push operations occur
	 * @param job the job ID for the operation
	 * @param groupingKey an optional set of grouping keys for the operation
	 * @param shutdownOperation the shutdown operation that should be performed when
	 * context is closed.
	 */
	public PrometheusPushGatewayManager(PushGateway pushGateway, CollectorRegistry registry, TaskScheduler scheduler,
			Duration pushRate, String job, Map<String, String> groupingKey, ShutdownOperation shutdownOperation) {
		Assert.notNull(pushGateway, "PushGateway must not be null");
		Assert.notNull(registry, "Registry must not be null");
		Assert.notNull(scheduler, "Scheduler must not be null");
		Assert.notNull(pushRate, "PushRate must not be null");
		Assert.hasLength(job, "Job must not be empty");
		this.pushGateway = pushGateway;
		this.registry = registry;
		this.job = job;
		this.groupingKey = groupingKey;
		this.shutdownOperation = (shutdownOperation != null) ? shutdownOperation : ShutdownOperation.NONE;
		this.scheduler = scheduler;
		this.scheduled = this.scheduler.scheduleAtFixedRate(this::post, pushRate);
	}

	/**
     * Posts the metrics to the Prometheus Pushgateway.
     * 
     * @throws Throwable if an unexpected exception occurs during the POST operation
     */
    private void post() {
		try {
			this.pushGateway.pushAdd(this.registry, this.job, this.groupingKey);
		}
		catch (Throwable ex) {
			logger.warn("Unexpected exception thrown by POST of metrics to Prometheus Pushgateway", ex);
		}
	}

	/**
     * Pushes the metrics to the Prometheus Pushgateway.
     * 
     * @throws Throwable if an unexpected exception occurs during the push operation
     */
    private void put() {
		try {
			this.pushGateway.push(this.registry, this.job, this.groupingKey);
		}
		catch (Throwable ex) {
			logger.warn("Unexpected exception thrown by PUT of metrics to Prometheus Pushgateway", ex);
		}
	}

	/**
     * Deletes metrics from the Prometheus Pushgateway.
     * 
     * @throws Throwable if an unexpected exception occurs during the deletion process
     */
    private void delete() {
		try {
			this.pushGateway.delete(this.job, this.groupingKey);
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

	/**
     * Shuts down the PrometheusPushGatewayManager.
     * 
     * @param shutdownOperation the operation to perform during shutdown (POST, PUT, or DELETE)
     */
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

		/**
         * Constructor for the PushGatewayTaskScheduler class.
         * Initializes the task scheduler with a pool size of 1 and sets it as a daemon thread.
         * Sets the thread group name as "prometheus-push-gateway".
         */
        PushGatewayTaskScheduler() {
			setPoolSize(1);
			setDaemon(true);
			setThreadGroupName("prometheus-push-gateway");
		}

		/**
         * Returns the scheduled executor service for this PushGatewayTaskScheduler.
         * 
         * @return the scheduled executor service
         * @throws IllegalStateException if the executor service is not available
         */
        @Override
		public ScheduledExecutorService getScheduledExecutor() throws IllegalStateException {
			return Executors.newSingleThreadScheduledExecutor(this::newThread);
		}

	}

}
