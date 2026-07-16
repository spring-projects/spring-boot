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

package org.springframework.boot.jms.health;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.convert.DurationStyle;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;

/**
 * {@link HealthIndicator} for a JMS {@link ConnectionFactory}.
 *
 * @author Stephane Nicoll
 * @author Venkata Naga Sai Srikanth Gollapudi
 * @since 4.0.0
 */
public class JmsHealthIndicator extends AbstractHealthIndicator {

	private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

	private final Log logger = LogFactory.getLog(JmsHealthIndicator.class);

	private final ConnectionFactory connectionFactory;

	private final Duration timeout;

	public JmsHealthIndicator(ConnectionFactory connectionFactory) {
		this(connectionFactory, DEFAULT_TIMEOUT);
	}

	public JmsHealthIndicator(ConnectionFactory connectionFactory, Duration timeout) {
		super("JMS health check failed");
		Assert.notNull(timeout, "'timeout' must not be null");
		Assert.isTrue(timeout.compareTo(Duration.ZERO) > 0, "'timeout' must be greater than 0");
		this.connectionFactory = connectionFactory;
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
			new Thread(() -> {
				try {
					if (!this.latch.await(JmsHealthIndicator.this.timeout.toNanos(), TimeUnit.NANOSECONDS)) {
						JmsHealthIndicator.this.logger
							.warn(LogMessage.format("Connection failed to start within %s and will be closed.",
									DurationStyle.SIMPLE.print(JmsHealthIndicator.this.timeout)));
						closeConnection();
					}
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}, "jms-health-indicator").start();
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
