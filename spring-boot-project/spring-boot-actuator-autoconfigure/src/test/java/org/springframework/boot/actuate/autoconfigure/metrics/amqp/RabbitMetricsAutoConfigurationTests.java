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

package org.springframework.boot.actuate.autoconfigure.metrics.amqp;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
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

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
		.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class, RabbitMetricsAutoConfiguration.class));

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
