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

import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StartupTimeMetrics}.
 *
 * @author Chris Bono
 */
class StartupTimeMetricsTests {

	private static final long APP_STARTED_TIME_MS = 2500;
	private static final long APP_RUNNING_TIME_MS = 2900;
	private static final long APP_RUNNING_JVM_UPTIME_MS  = 3200;

	private MeterRegistry registry;
	private RuntimeMXBean runtimeMXBean;
	private StartupTimeMetrics metrics;

	@BeforeEach
	void prepareUnit() {
		registry = new SimpleMeterRegistry();
		runtimeMXBean = mock(RuntimeMXBean.class);
		given(runtimeMXBean.getUptime()).willReturn(APP_RUNNING_JVM_UPTIME_MS);
		metrics = new StartupTimeMetrics(registry, runtimeMXBean);
	}

	@Test
	void metricsRecordedWithoutCustomTags() {
		metrics.onApplicationEvent(applicationStartedEvent(APP_STARTED_TIME_MS));
		metrics.onApplicationEvent(applicationReadyEvent(APP_RUNNING_TIME_MS));
		assertMetricExistsWithValue("spring.boot.startup.app.started", APP_STARTED_TIME_MS);
		assertMetricExistsWithValue("spring.boot.startup.app.running", APP_RUNNING_TIME_MS);
		assertMetricExistsWithValue("spring.boot.startup.app.running.jvm", APP_RUNNING_JVM_UPTIME_MS);
	}

	@Test
	void metricsRecordedWithCustomTags() {
		Tags tags = Tags.of("foo", "bar");
		metrics = new StartupTimeMetrics(registry, runtimeMXBean, tags);
		metrics.onApplicationEvent(applicationStartedEvent(APP_STARTED_TIME_MS));
		metrics.onApplicationEvent(applicationReadyEvent(APP_RUNNING_TIME_MS));
		assertMetricExistsWithCustomTagsAndValue("spring.boot.startup.app.started", tags, APP_STARTED_TIME_MS);
		assertMetricExistsWithCustomTagsAndValue("spring.boot.startup.app.running", tags, APP_RUNNING_TIME_MS);
		assertMetricExistsWithCustomTagsAndValue("spring.boot.startup.app.running.jvm", tags, APP_RUNNING_JVM_UPTIME_MS);
	}

	@Test
	void metricsRecordedWithoutMainAppClassTagWhenMainAppClassNotAvailable() {
		metrics.onApplicationEvent(applicationStartedEvent(APP_STARTED_TIME_MS));
		metrics.onApplicationEvent(applicationReadyEvent(APP_RUNNING_TIME_MS));
		assertThat(registry.find("spring.boot.startup.app.started").timeGauge()).isNotNull();
		assertThat(registry.find("spring.boot.startup.app.running").timeGauge()).isNotNull();
		assertThat(registry.find("spring.boot.startup.app.running.jvm").timeGauge()).isNotNull();
	}

	@Test
	void metricsNotRecordedWhenStartupTimeNotAvailable() {
		metrics.onApplicationEvent(applicationStartedEvent(null));
		metrics.onApplicationEvent(applicationReadyEvent(null));
		assertThat(registry.find("spring.boot.startup.app.started").timeGauge()).isNull();
		assertThat(registry.find("spring.boot.startup.app.running").timeGauge()).isNull();
		assertThat(registry.find("spring.boot.startup.app.running.jvm").timeGauge()).isNull();
	}

	private ApplicationStartedEvent applicationStartedEvent(Long startupTimeMs) {
		SpringApplication application = mock(SpringApplication.class);
		doReturn(TestMainApplication.class).when(application).getMainApplicationClass();
		return new ApplicationStartedEvent(application, null, null, startupTimeMs != null ? Duration.ofMillis(startupTimeMs) : null);
	}

	private ApplicationReadyEvent applicationReadyEvent(Long startupTimeMs) {
		SpringApplication application = mock(SpringApplication.class);
		doReturn(TestMainApplication.class).when(application).getMainApplicationClass();
		return new ApplicationReadyEvent(application, null, null, startupTimeMs != null ? Duration.ofMillis(startupTimeMs) : null);
	}

	private void assertMetricExistsWithValue(String metricName, double expectedValueInMillis) {
		assertMetricExistsWithCustomTagsAndValue(metricName, Tags.empty(), expectedValueInMillis);
	}

	private void assertMetricExistsWithCustomTagsAndValue(String metricName, Tags expectedCustomTags, double expectedValueInMillis) {
		assertThat(registry.find(metricName)
				.tags(Tags.concat(expectedCustomTags, "main-application-class", TestMainApplication.class.getName()))
				.timeGauge()).isNotNull()
				.extracting((m) -> m.value(TimeUnit.MILLISECONDS)).isEqualTo(expectedValueInMillis);
	}

	static class TestMainApplication {

	}

}
