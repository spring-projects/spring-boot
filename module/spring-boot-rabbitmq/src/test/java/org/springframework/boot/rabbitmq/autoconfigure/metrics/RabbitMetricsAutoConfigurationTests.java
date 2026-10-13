/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RabbitMetricsAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class RabbitMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(SimpleMeterRegistry.class)
		.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class, RabbitMetricsAutoConfiguration.class,
				MetricsAutoConfiguration.class))
		.withPropertyValues("management.metrics.use-global-registry=false");

	@Test
	void autoConfiguredConnectionFactoryIsInstrumented() {
		this.contextRunner.run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			registry.get("rabbitmq.connections").meter();
		});
	}

	@Test
	void abstractConnectionFactoryDefinedAsAConnectionFactoryIsInstrumented() {
		this.contextRunner.withUserConfiguration(ConnectionFactoryConfiguration.class).run((context) -> {
			assertThat(context).hasBean("customConnectionFactory");
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			registry.get("rabbitmq.connections").meter();
		});
	}

	@Test
	void rabbitmqNativeConnectionFactoryInstrumentationCanBeDisabled() {
		this.contextRunner.withPropertyValues("management.metrics.enable.rabbitmq=false").run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			assertThat(registry.find("rabbitmq.connections").meter()).isNull();
		});
	}

	@Configuration
	static class ConnectionFactoryConfiguration {

		@Bean
		ConnectionFactory customConnectionFactory() {
			return new CachingConnectionFactory();
		}

	}

}
