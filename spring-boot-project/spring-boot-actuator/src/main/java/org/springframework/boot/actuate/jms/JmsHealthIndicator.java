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

package org.springframework.boot.actuate.jms;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.log.LogMessage;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * {@link HealthIndicator} for a JMS {@link ConnectionFactory}.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @since 2.0.0
 */
public class JmsHealthIndicator extends AbstractHealthIndicator {

	private final Log logger = LogFactory.getLog(JmsHealthIndicator.class);

	private final ConnectionFactory connectionFactory;

	private final AsyncTaskExecutor taskExecutor;

	private final Duration timeout;

	/**
	 * Creates a new {@link JmsHealthIndicator}, using a {@link SimpleAsyncTaskExecutor}
	 * and a timeout of 5 seconds.
	 * @param connectionFactory the connection factory
	 * @deprecated since 3.2.0 for removal in 3.4.0 in favor of
	 * {@link #JmsHealthIndicator(ConnectionFactory, AsyncTaskExecutor, Duration)}
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public JmsHealthIndicator(ConnectionFactory connectionFactory) {
		this(connectionFactory, new SimpleAsyncTaskExecutor("jms-health-indicator"), Duration.ofSeconds(5));
	}

	/**
	 * Creates a new {@link JmsHealthIndicator}.
	 * @param connectionFactory the connection factory
	 * @param taskExecutor the task executor used to run timeout checks
	 * @param timeout the connection timeout
	 */
	public JmsHealthIndicator(ConnectionFactory connectionFactory, AsyncTaskExecutor taskExecutor, Duration timeout) {
		super("JMS health check failed");
		this.connectionFactory = connectionFactory;
		this.taskExecutor = taskExecutor;
		this.timeout = timeout;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		try (Connection connection = this.connectionFactory.createConnection()) {
			new MonitoredConnection(connection).start();
			builder.up().withDetail("provider", connection.getMetaData().getJMSProviderName());
		}
	}

	private final class MonitoredConnection {

		private final CountDownLatch latch = new CountDownLatch(1);

		private final Connection connection;

		MonitoredConnection(Connection connection) {
			this.connection = connection;
		}

		void start() throws JMSException {
			JmsHealthIndicator.this.taskExecutor.execute(() -> {
				try {
					if (!this.latch.await(JmsHealthIndicator.this.timeout.toMillis(), TimeUnit.MILLISECONDS)) {
						JmsHealthIndicator.this.logger
							.warn(LogMessage.format("Connection failed to start within %s and will be closed.",
									JmsHealthIndicator.this.timeout));
						closeConnection();
					}
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			});
			this.connection.start();
			this.latch.countDown();
		}

		private void closeConnection() {
			try {
				this.connection.close();
			}
			catch (Exception ex) {
				// Continue
			}
		}

	}

}
