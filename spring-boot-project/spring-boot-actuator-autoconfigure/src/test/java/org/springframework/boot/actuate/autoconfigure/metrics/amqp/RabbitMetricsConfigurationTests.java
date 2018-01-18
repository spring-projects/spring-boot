/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.amqp;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RabbitMetricsConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class RabbitMetricsConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(RegistryConfiguration.class)
			.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class,
					RabbitAutoConfiguration.class))
			.withPropertyValues("management.metrics.use-global-registry=false");


	@Test
	public void autoConfiguredConnectionFactoryIsInstrumented() {
		this.contextRunner.run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			assertThat(registry.find("rabbitmq.connections").meter()).isPresent();
		});
	}

	@Test
	public void autoConfiguredConnectionFactoryWithCustomMetricName() {
		this.contextRunner
				.withPropertyValues("management.metrics.rabbitmq.metric-name=custom.name")
				.run((context) -> {
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find("custom.name.connections").meter())
							.isPresent();
					assertThat(registry.find("rabbitmq.connections").meter())
							.isNotPresent();
				});
	}

	@Test
	public void rabbitmqNativeConnectionFactoryInstrumentationCanBeDisabled() {
		this.contextRunner
				.withPropertyValues(
						"management.metrics.rabbitmq.instrument=false").run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			assertThat(registry.find("rabbitmq.connections").meter()).isNotPresent();
		});
	}


	@Configuration
	static class RegistryConfiguration {

		@Bean
		public MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

	}

}
