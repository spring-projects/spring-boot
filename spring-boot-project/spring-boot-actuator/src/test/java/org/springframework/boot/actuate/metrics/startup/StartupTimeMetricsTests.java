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

package org.springframework.boot.actuate.metrics.startup;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StartupTimeMetrics}.
 *
 * @author Chris Bono
 */
class StartupTimeMetricsTests {

	@Nested
	class WhenApplicationStartedEvent {

		@Test
		void metricRecordedWithoutAdditionalTags() {
			SimpleMeterRegistry registry = new SimpleMeterRegistry();
			StartupTimeMetrics metrics = new StartupTimeMetrics(registry);
			metrics.onApplicationEvent(applicationStartedEvent(TestMainApplication.class, Duration.ofMillis(2500)));
			assertThat(registry.find("spring.boot.application.started")
					.tag("main-application-class", TestMainApplication.class.getName()).timeGauge()).isNotNull()
							.extracting((m) -> m.value(TimeUnit.MILLISECONDS)).isEqualTo(2500d);
		}

		@Test
		void metricRecordedWithAdditionalTags() {
			SimpleMeterRegistry registry = new SimpleMeterRegistry();
			StartupTimeMetrics metrics = new StartupTimeMetrics(registry, Tags.of("foo", "bar"));
			metrics.onApplicationEvent(applicationStartedEvent(TestMainApplication.class, Duration.ofMillis(2500)));
			assertThat(registry.find("spring.boot.application.started")
					.tags("main-application-class", TestMainApplication.class.getName(), "foo", "bar").timeGauge())
							.isNotNull().extracting((m) -> m.value(TimeUnit.MILLISECONDS)).isEqualTo(2500d);
		}

		@Test
		void metricRecordedWithoutMainAppClassTagWhenMainAppClassNotAvailable() {
			SimpleMeterRegistry registry = new SimpleMeterRegistry();
			StartupTimeMetrics metrics = new StartupTimeMetrics(registry);
			metrics.onApplicationEvent(applicationStartedEvent(null, Duration.ofMillis(2500)));
			assertThat(registry.find("spring.boot.application.started").timeGauge()).isNotNull();
		}

		@Test
		void metricNotRecordedWhenStartupTimeNotAvailable() {
			SimpleMeterRegistry registry = new SimpleMeterRegistry();
			StartupTimeMetrics metrics = new StartupTimeMetrics(registry);
			metrics.onApplicationEvent(applicationStartedEvent(null, null));
			assertThat(registry.find("spring.boot.application.started").timeGauge()).isNull();
		}

		private ApplicationStartedEvent applicationStartedEvent(Class<?> mainAppClass, Duration startupTime) {
			SpringApplication application = mock(SpringApplication.class);
			doReturn(mainAppClass).when(application).getMainApplicationClass();
			return new ApplicationStartedEvent(application, null, null, startupTime);
		}

	}

	@Nested
	class WhenApplicationReadyEvent {

		@Test
		void metricRecordedWithoutAdditionalTags() {
			SimpleMeterRegistry registry = new SimpleMeterRegistry();
			StartupTimeMetrics metrics = new StartupTimeMetrics(registry);
			metrics.onApplicationEvent(applicationReadyEvent(TestMainApplication.class, Duration.ofMillis(2500)));
			assertThat(registry.find("spring.boot.application.running")
					.tag("main-application-class", TestMainApplication.class.getName()).timeGauge()).isNotNull()
							.extracting((m) -> m.value(TimeUnit.MILLISECONDS)).isEqualTo(2500d);
		}

		@Test
		void metricRecordedWithAdditionalTags() {
			SimpleMeterRegistry registry = new SimpleMeterRegistry();
			StartupTimeMetrics metrics = new StartupTimeMetrics(registry, Tags.of("foo", "bar"));
			metrics.onApplicationEvent(applicationReadyEvent(TestMainApplication.class, Duration.ofMillis(2500)));
			assertThat(registry.find("spring.boot.application.running")
					.tags("main-application-class", TestMainApplication.class.getName(), "foo", "bar").timeGauge())
							.isNotNull().extracting((m) -> m.value(TimeUnit.MILLISECONDS)).isEqualTo(2500d);
		}

		@Test
		void metricRecordedWithoutMainAppClassTagWhenMainAppClassNotAvailable() {
			SimpleMeterRegistry registry = new SimpleMeterRegistry();
			StartupTimeMetrics metrics = new StartupTimeMetrics(registry);
			metrics.onApplicationEvent(applicationReadyEvent(null, Duration.ofMillis(2500)));
			assertThat(registry.find("spring.boot.application.running").timeGauge()).isNotNull();
		}

		@Test
		void metricNotRecordedWhenStartupTimeNotAvailable() {
			SimpleMeterRegistry registry = new SimpleMeterRegistry();
			StartupTimeMetrics metrics = new StartupTimeMetrics(registry);
			metrics.onApplicationEvent(applicationReadyEvent(null, null));
			assertThat(registry.find("spring.boot.application.running").timeGauge()).isNull();
		}

		private ApplicationReadyEvent applicationReadyEvent(Class<?> mainAppClass, Duration startupTime) {
			SpringApplication application = mock(SpringApplication.class);
			doReturn(mainAppClass).when(application).getMainApplicationClass();
			return new ApplicationReadyEvent(application, null, null, startupTime);
		}

	}

	static class TestMainApplication {

	}

}
