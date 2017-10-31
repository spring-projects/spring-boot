/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.jms;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * {@link HealthIndicator} for a JMS {@link ConnectionFactory}.
 *
 * @author Stephane Nicoll
 * @author Filip Hrisfov
 * @since 2.0.0
 */
public class JmsHealthIndicator extends AbstractHealthIndicator {

	private final ConnectionFactory connectionFactory;

	public JmsHealthIndicator(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		try (Connection connection = this.connectionFactory.createConnection()) {
			CompletableFuture<Exception> future = CompletableFuture.supplyAsync(() -> {
				try {
					connection.start();
					return null;
				}
				catch (JMSException e) {
					return e;
				}
			});
			try {
				Exception exception = future.get(100, TimeUnit.MILLISECONDS);
				if (exception != null) {
					throw exception;
				}
				builder.up().withDetail("provider",
						connection.getMetaData().getJMSProviderName());
			}
			catch (TimeoutException ex) {
				builder.unknown().withDetail("provider",
						connection.getMetaData().getJMSProviderName())
						.withDetail("cause", "Could not connect for 100 milliseconds");
			}
			catch (ExecutionException ex) {
				throw ex;
			}
		}
	}

}
