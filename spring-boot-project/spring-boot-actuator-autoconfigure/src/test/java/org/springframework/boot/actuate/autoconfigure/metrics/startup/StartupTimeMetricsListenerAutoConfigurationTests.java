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

package org.springframework.boot.actuate.autoconfigure.metrics.startup;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.actuate.metrics.startup.StartupTimeMetricsListener;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StartupTimeMetricsListenerAutoConfiguration}.
 *
 * @author Chris Bono
 * @author Stephane Nicoll
 */
class StartupTimeMetricsListenerAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
		.withConfiguration(AutoConfigurations.of(StartupTimeMetricsListenerAutoConfiguration.class));

	@Test
	void startupTimeMetricsAreRecorded() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(StartupTimeMetricsListener.class);
			SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
			context.publishEvent(new ApplicationStartedEvent(new SpringApplication(), null,
					context.getSourceApplicationContext(), Duration.ofMillis(1500)));
			TimeGauge startedTimeGage = registry.find("application.started.time").timeGauge();
			assertThat(startedTimeGage).isNotNull();
			assertThat(startedTimeGage.value(TimeUnit.MILLISECONDS)).isEqualTo(1500L);
			context.publishEvent(new ApplicationReadyEvent(new SpringApplication(), null,
					context.getSourceApplicationContext(), Duration.ofMillis(2000)));
			TimeGauge readyTimeGage = registry.find("application.ready.time").timeGauge();
			assertThat(readyTimeGage).isNotNull();
			assertThat(readyTimeGage.value(TimeUnit.MILLISECONDS)).isEqualTo(2000L);
		});
	}

	@Test
	void startupTimeMetricsCanBeDisabled() {
		this.contextRunner
			.withPropertyValues("management.metrics.enable.application.started.time:false",
					"management.metrics.enable.application.ready.time:false")
			.run((context) -> {
				context.publishEvent(new ApplicationStartedEvent(new SpringApplication(), null,
						context.getSourceApplicationContext(), Duration.ofMillis(2500)));
				context.publishEvent(new ApplicationReadyEvent(new SpringApplication(), null,
						context.getSourceApplicationContext(), Duration.ofMillis(3000)));
				SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
				assertThat(registry.find("application.started.time").timeGauge()).isNull();
				assertThat(registry.find("application.ready.time").timeGauge()).isNull();
			});
	}

	@Test
	void customStartupTimeMetricsAreRespected() {
		this.contextRunner
			.withBean("customStartupTimeMetrics", StartupTimeMetricsListener.class,
					() -> mock(StartupTimeMetricsListener.class))
			.run((context) -> assertThat(context).hasSingleBean(StartupTimeMetricsListener.class)
				.hasBean("customStartupTimeMetrics"));
	}

}
