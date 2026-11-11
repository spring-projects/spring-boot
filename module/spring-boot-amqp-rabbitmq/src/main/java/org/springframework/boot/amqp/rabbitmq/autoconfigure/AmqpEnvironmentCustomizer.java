/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.rabbitmq.autoconfigure;

import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * auto-configured AMQP {@link Environment} that is created by an
 * {@link AmqpEnvironmentBuilder}.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 4.2.0
 */
@FunctionalInterface
public interface AmqpEnvironmentCustomizer {

	/**
	 * Customize the {@link AmqpEnvironmentBuilder}.
	 * @param environmentBuilder the environment builder to customize
	 */
	void customize(AmqpEnvironmentBuilder environmentBuilder);

}
