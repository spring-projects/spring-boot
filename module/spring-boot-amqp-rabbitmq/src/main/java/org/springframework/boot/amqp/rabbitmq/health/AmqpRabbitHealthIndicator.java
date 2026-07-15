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

package org.springframework.boot.amqp.rabbitmq.health;

import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.util.Assert;

/**
 * {@link HealthIndicator} for AMQP 1.0 support with RabbitMQ.
 *
 * @author Stephane Nicoll
 * @since 4.2.0
 */
public class AmqpRabbitHealthIndicator extends AbstractHealthIndicator {

	private final AmqpConnectionFactory amqpConnectionFactory;

	public AmqpRabbitHealthIndicator(AmqpConnectionFactory amqpConnectionFactory) {
		super("Rabbit health check failed");
		Assert.notNull(amqpConnectionFactory, "'amqpConnectionFactory' must not be null");
		this.amqpConnectionFactory = amqpConnectionFactory;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		builder.up();
		String version = this.amqpConnectionFactory.getConnection().connectionInfo().brokerVersion();
		if (version != null) {
			builder.withDetail("version", version);
		}
	}

}
