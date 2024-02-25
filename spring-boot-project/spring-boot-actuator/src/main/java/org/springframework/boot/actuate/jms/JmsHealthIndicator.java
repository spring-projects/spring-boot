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

/**
 * {@link HealthIndicator} for a JMS {@link ConnectionFactory}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class JmsHealthIndicator extends AbstractHealthIndicator {

	private final Log logger = LogFactory.getLog(JmsHealthIndicator.class);

	private final ConnectionFactory connectionFactory;

	/**
     * Constructs a new JmsHealthIndicator with the specified ConnectionFactory.
     * 
     * @param connectionFactory the ConnectionFactory to be used for JMS health check
     */
    public JmsHealthIndicator(ConnectionFactory connectionFactory) {
		super("JMS health check failed");
		this.connectionFactory = connectionFactory;
	}

	/**
     * Performs a health check on the JMS connection.
     * 
     * @param builder the Health.Builder object used to build the health status
     * @throws Exception if an error occurs during the health check
     */
    @Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		try (Connection connection = this.connectionFactory.createConnection()) {
			new MonitoredConnection(connection).start();
			builder.up().withDetail("provider", connection.getMetaData().getJMSProviderName());
		}
	}

	/**
     * MonitoredConnection class.
     */
    private final class MonitoredConnection {

		private final CountDownLatch latch = new CountDownLatch(1);

		private final Connection connection;

		/**
         * Creates a new MonitoredConnection object with the specified Connection.
         * 
         * @param connection the Connection object to be monitored
         */
        MonitoredConnection(Connection connection) {
			this.connection = connection;
		}

		/**
         * Starts the connection and waits for it to start within 5 seconds.
         * If the connection fails to start within the specified time, it will be closed.
         *
         * @throws JMSException if there is an error starting the connection
         */
        void start() throws JMSException {
			new Thread(() -> {
				try {
					if (!this.latch.await(5, TimeUnit.SECONDS)) {
						JmsHealthIndicator.this.logger
							.warn("Connection failed to start within 5 seconds and will be closed.");
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

		/**
         * Closes the connection to the database.
         * 
         * @throws Exception if an error occurs while closing the connection
         */
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
