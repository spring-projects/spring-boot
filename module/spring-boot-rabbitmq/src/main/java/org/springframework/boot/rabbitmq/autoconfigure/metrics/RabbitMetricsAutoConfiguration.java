/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure.metrics;

import com.rabbitmq.client.ConnectionFactory;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.amqp.rabbit.connection.AbstractConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all available
 * {@link ConnectionFactory connection factories}.
 *
 * @author Stephane Nicoll
 * @since 4.2.0
 */
@AutoConfiguration(
		afterName = "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
		after = RabbitAutoConfiguration.class)
@ConditionalOnClass({ ConnectionFactory.class, AbstractConnectionFactory.class, MeterRegistry.class })
@ConditionalOnBean({ org.springframework.amqp.rabbit.connection.ConnectionFactory.class, MeterRegistry.class })
public final class RabbitMetricsAutoConfiguration {

	@Bean
	static RabbitConnectionFactoryMetricsPostProcessor rabbitConnectionFactoryMetricsPostProcessor(
			ApplicationContext applicationContext) {
		return new RabbitConnectionFactoryMetricsPostProcessor(applicationContext);
	}

}
