/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.startup;

import java.lang.management.ManagementFactory;
import java.time.Duration;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.actuate.metrics.startup.StartupTimeMetrics;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StartupTimeMetricsAutoConfiguration}.
 *
 * @author Chris Bono
 */
class StartupTimeMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(StartupTimeMetricsAutoConfiguration.class));

	@Test
	void startupTimeMetricsAreRecorded() {
		this.contextRunner.run((context) -> {
			context.publishEvent(new ApplicationStartedEvent(new SpringApplication(), null,
					context.getSourceApplicationContext(), Duration.ofMillis(2500)));
			context.publishEvent(new ApplicationReadyEvent(new SpringApplication(), null,
					context.getSourceApplicationContext(), Duration.ofMillis(3000)));
			assertThat(context).hasSingleBean(StartupTimeMetrics.class);
			SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
			assertThat(registry.find("application.started.time").timeGauge()).isNotNull();
			assertThat(registry.find("application.ready.time").timeGauge()).isNotNull();
			assertThat(registry.find("application.ready.jvm.time").timeGauge()).isNotNull();
		});
	}

	@Test
	void startupTimeMetricsCanBeDisabled() {
		this.contextRunner.withPropertyValues("management.metrics.enable.application.started.time:false",
				"management.metrics.enable.application.ready.time:false", "management.metrics.enable.application.ready.jvm.time:false")
				.run((context) -> {
					context.publishEvent(new ApplicationStartedEvent(new SpringApplication(), null,
							context.getSourceApplicationContext(), Duration.ofMillis(2500)));
					context.publishEvent(new ApplicationReadyEvent(new SpringApplication(), null,
							context.getSourceApplicationContext(), Duration.ofMillis(3000)));
					SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
					assertThat(registry.find("application.started.time").timeGauge()).isNull();
					assertThat(registry.find("application.ready.time").timeGauge()).isNull();
					assertThat(registry.find("application.ready.jvm.time").timeGauge()).isNull();
				});
	}

	@Test
	void customStartupTimeMetricsAreRespected() {
		this.contextRunner.withUserConfiguration(CustomStartupTimeMetricsConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(StartupTimeMetrics.class)
						.hasBean("customStartTimeMetrics"));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomStartupTimeMetricsConfiguration {

		@Bean
		StartupTimeMetrics customStartTimeMetrics() {
			return new StartupTimeMetrics(new SimpleMeterRegistry(), ManagementFactory.getRuntimeMXBean());
		}

	}

}
