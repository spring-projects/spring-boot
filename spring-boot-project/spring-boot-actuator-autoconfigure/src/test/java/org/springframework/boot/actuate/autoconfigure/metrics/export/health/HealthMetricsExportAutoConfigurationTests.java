/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.health;

import io.micrometer.core.instrument.Clock;
import io.micrometer.health.HealthConfig;
import io.micrometer.health.HealthMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthMetricsExportAutoConfiguration}.
 *
 * @author Jon Schneider
 */
class HealthMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HealthMetricsExportAutoConfiguration.class));

	@Test
	void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(HealthMeterRegistry.class));
	}

	@Test
	void autoConfiguresConfigAndMeterRegistry() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class).run((context) -> assertThat(context)
				.hasSingleBean(HealthMeterRegistry.class).hasSingleBean(HealthConfig.class));
	}

	@Test
	void autoConfiguresHealthIndicators() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.health.api-error-budgets.api.customer=0.01")
				.withPropertyValues("management.metrics.export.health.api-error-budgets.admin=0.02")
				.run((context) -> assertThat(context).hasBean("apiErrorRatioApiCustomer").hasBean("apiErrorRatioAdmin")
						.hasBean("jvmGcLoad").hasBean("jvmPoolMemory").hasBean("jvmTotalMemory"));
	}

	@Test
	void autoConfigurationCanBeDisabled() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.health.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean(HealthMeterRegistry.class)
						.doesNotHaveBean(HealthConfig.class));
	}

	@Test
	void allowsCustomConfigToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class).run((context) -> assertThat(context)
				.hasSingleBean(HealthMeterRegistry.class).hasSingleBean(HealthConfig.class).hasBean("customConfig"));
	}

	@Test
	void allowsCustomRegistryToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomRegistryConfiguration.class).run((context) -> assertThat(context)
				.hasSingleBean(HealthMeterRegistry.class).hasBean("customRegistry").hasSingleBean(HealthConfig.class));
	}

	@Test
	void stopsMeterRegistryWhenContextIsClosed() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class).run((context) -> {
			HealthMeterRegistry registry = context.getBean(HealthMeterRegistry.class);
			assertThat(registry.isClosed()).isFalse();
			context.close();
			assertThat(registry.isClosed()).isTrue();
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		Clock clock() {
			return Clock.SYSTEM;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomConfigConfiguration {

		@Bean
		HealthConfig customConfig() {
			return (key) -> {
				if ("health.step".equals(key)) {
					return "PT20S";
				}
				return null;
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		HealthMeterRegistry customRegistry(HealthConfig config, Clock clock) {
			return HealthMeterRegistry.builder(config).clock(clock).build();
		}

	}

}
