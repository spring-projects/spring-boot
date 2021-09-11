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

	private static final long APP_RUNNING_JVM_UPTIME_MS = 3200;

	private MeterRegistry registry;

	private RuntimeMXBean runtimeMXBean;

	private StartupTimeMetrics metrics;

	@BeforeEach
	void prepareUnit() {
		this.registry = new SimpleMeterRegistry();
		this.runtimeMXBean = mock(RuntimeMXBean.class);
		given(this.runtimeMXBean.getUptime()).willReturn(APP_RUNNING_JVM_UPTIME_MS);
		this.metrics = new StartupTimeMetrics(this.registry, this.runtimeMXBean, "application.started.time", "application.ready.time",
				"application.ready.jvm.time");
	}

	@Test
	void metricsRecordedWithoutCustomTags() {
		this.metrics.onApplicationEvent(applicationStartedEvent(APP_STARTED_TIME_MS));
		this.metrics.onApplicationEvent(applicationReadyEvent(APP_RUNNING_TIME_MS));
		assertMetricExistsWithValue("application.started.time", APP_STARTED_TIME_MS);
		assertMetricExistsWithValue("application.ready.time", APP_RUNNING_TIME_MS);
		assertMetricExistsWithValue("application.ready.jvm.time", APP_RUNNING_JVM_UPTIME_MS);
	}

	@Test
	void metricsRecordedWithCustomTags() {
		Tags tags = Tags.of("foo", "bar");
		this.metrics = new StartupTimeMetrics(this.registry, this.runtimeMXBean, tags, "application.started.time",
				"application.ready.time", "application.ready.jvm.time");
		this.metrics.onApplicationEvent(applicationStartedEvent(APP_STARTED_TIME_MS));
		this.metrics.onApplicationEvent(applicationReadyEvent(APP_RUNNING_TIME_MS));
		assertMetricExistsWithCustomTagsAndValue("application.started.time", tags, APP_STARTED_TIME_MS);
		assertMetricExistsWithCustomTagsAndValue("application.ready.time", tags, APP_RUNNING_TIME_MS);
		assertMetricExistsWithCustomTagsAndValue("application.ready.jvm.time", tags, APP_RUNNING_JVM_UPTIME_MS);
	}

	@Test
	void metricsRecordedWithoutMainAppClassTagWhenMainAppClassNotAvailable() {
		this.metrics.onApplicationEvent(applicationStartedEvent(APP_STARTED_TIME_MS));
		this.metrics.onApplicationEvent(applicationReadyEvent(APP_RUNNING_TIME_MS));
		assertThat(this.registry.find("application.started.time").timeGauge()).isNotNull();
		assertThat(this.registry.find("application.ready.time").timeGauge()).isNotNull();
		assertThat(this.registry.find("application.ready.jvm.time").timeGauge()).isNotNull();
	}

	@Test
	void metricsNotRecordedWhenStartupTimeNotAvailable() {
		this.metrics.onApplicationEvent(applicationStartedEvent(null));
		this.metrics.onApplicationEvent(applicationReadyEvent(null));
		assertThat(this.registry.find("application.started.time").timeGauge()).isNull();
		assertThat(this.registry.find("application.ready.time").timeGauge()).isNull();
		assertThat(this.registry.find("application.ready.jvm.time").timeGauge()).isNull();
	}

	private ApplicationStartedEvent applicationStartedEvent(Long startupTimeMs) {
		SpringApplication application = mock(SpringApplication.class);
		doReturn(TestMainApplication.class).when(application).getMainApplicationClass();
		return new ApplicationStartedEvent(application, null, null,
				(startupTimeMs != null) ? Duration.ofMillis(startupTimeMs) : null);
	}

	private ApplicationReadyEvent applicationReadyEvent(Long startupTimeMs) {
		SpringApplication application = mock(SpringApplication.class);
		doReturn(TestMainApplication.class).when(application).getMainApplicationClass();
		return new ApplicationReadyEvent(application, null, null,
				(startupTimeMs != null) ? Duration.ofMillis(startupTimeMs) : null);
	}

	private void assertMetricExistsWithValue(String metricName, double expectedValueInMillis) {
		assertMetricExistsWithCustomTagsAndValue(metricName, Tags.empty(), expectedValueInMillis);
	}

	private void assertMetricExistsWithCustomTagsAndValue(String metricName, Tags expectedCustomTags,
			double expectedValueInMillis) {
		assertThat(this.registry.find(metricName)
				.tags(Tags.concat(expectedCustomTags, "main-application-class", TestMainApplication.class.getName()))
				.timeGauge()).isNotNull().extracting((m) -> m.value(TimeUnit.MILLISECONDS))
						.isEqualTo(expectedValueInMillis);
	}

	static class TestMainApplication {

	}

}
