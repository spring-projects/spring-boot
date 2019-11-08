/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.graphite.GraphiteMeterRegistry;
import io.micrometer.jmx.JmxMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.graphite.GraphiteMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.jmx.JmxMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for metrics auto-configuration.
 *
 * @author Stephane Nicoll
 */
class MetricsAutoConfigurationIntegrationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple());

	@Test
	void propertyBasedMeterFilteringIsAutoConfigured() {
		this.contextRunner.withPropertyValues("management.metrics.enable.my.org=false").run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			registry.timer("my.org.timer");
			assertThat(registry.find("my.org.timer").timer()).isNull();
		});
	}

	@Test
	void propertyBasedCommonTagsIsAutoConfigured() {
		this.contextRunner
				.withPropertyValues("management.metrics.tags.region=test", "management.metrics.tags.origin=local")
				.run((context) -> {
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					registry.counter("my.counter", "env", "qa");
					assertThat(registry.find("my.counter").tags("env", "qa").tags("region", "test")
							.tags("origin", "local").counter()).isNotNull();
				});
	}

	@Test
	void simpleMeterRegistryIsUsedAsAFallback() {
		this.contextRunner.run(
				(context) -> assertThat(context.getBean(MeterRegistry.class)).isInstanceOf(SimpleMeterRegistry.class));
	}

	@Test
	void emptyCompositeIsCreatedWhenNoMeterRegistriesAreAutoConfigured() {
		new ApplicationContextRunner().with(MetricsRun.limitedTo()).run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			assertThat(registry).isInstanceOf(CompositeMeterRegistry.class);
			assertThat(((CompositeMeterRegistry) registry).getRegistries()).isEmpty();
		});
	}

	@Test
	void noCompositeIsCreatedWhenASingleMeterRegistryIsAutoConfigured() {
		new ApplicationContextRunner().with(MetricsRun.limitedTo(GraphiteMetricsExportAutoConfiguration.class))
				.run((context) -> assertThat(context.getBean(MeterRegistry.class))
						.isInstanceOf(GraphiteMeterRegistry.class));
	}

	@Test
	void noCompositeIsCreatedWithMultipleRegistriesAndOneThatIsPrimary() {
		new ApplicationContextRunner()
				.with(MetricsRun.limitedTo(GraphiteMetricsExportAutoConfiguration.class,
						JmxMetricsExportAutoConfiguration.class))
				.withUserConfiguration(PrimaryMeterRegistryConfiguration.class)
				.run((context) -> assertThat(context.getBean(MeterRegistry.class))
						.isInstanceOf(SimpleMeterRegistry.class));
	}

	@Test
	void compositeCreatedWithMultipleRegistries() {
		new ApplicationContextRunner().with(MetricsRun.limitedTo(GraphiteMetricsExportAutoConfiguration.class,
				JmxMetricsExportAutoConfiguration.class)).run((context) -> {
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry).isInstanceOf(CompositeMeterRegistry.class);
					assertThat(((CompositeMeterRegistry) registry).getRegistries())
							.hasAtLeastOneElementOfType(GraphiteMeterRegistry.class)
							.hasAtLeastOneElementOfType(JmxMeterRegistry.class);
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class PrimaryMeterRegistryConfiguration {

		@Primary
		@Bean
		MeterRegistry simpleMeterRegistry() {
			return new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());
		}

	}

}
