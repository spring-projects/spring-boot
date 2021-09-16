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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StartupTimeMetrics}.
 *
 * @author Chris Bono
 */
class StartupTimeMetricsTests {

	private MeterRegistry registry;

	private StartupTimeMetrics metrics;

	@BeforeEach
	void setup() {
		this.registry = new SimpleMeterRegistry();
		this.metrics = new StartupTimeMetrics(this.registry);
	}

	@Test
	void metricsRecordedWithoutCustomTags() {
		this.metrics.onApplicationEvent(applicationStartedEvent(2000L));
		this.metrics.onApplicationEvent(applicationReadyEvent(2200L));
		assertMetricExistsWithValue("application.started.time", 2000L);
		assertMetricExistsWithValue("application.ready.time", 2200L);
	}

	@Test
	void metricsRecordedWithCustomTagsAndMetricNames() {
		Tags tags = Tags.of("foo", "bar");
		this.metrics = new StartupTimeMetrics(this.registry, tags, "m1", "m2");
		this.metrics.onApplicationEvent(applicationStartedEvent(1000L));
		this.metrics.onApplicationEvent(applicationReadyEvent(1050L));
		assertMetricExistsWithCustomTagsAndValue("m1", tags, 1000L);
		assertMetricExistsWithCustomTagsAndValue("m2", tags, 1050L);
	}

	@Test
	void metricRecordedWithoutMainAppClassTag() {
		SpringApplication application = mock(SpringApplication.class);
		this.metrics.onApplicationEvent(new ApplicationStartedEvent(application, null, null, Duration.ofSeconds(2)));
		TimeGauge applicationStartedGague = this.registry.find("application.started.time").timeGauge();
		assertThat(applicationStartedGague).isNotNull();
		assertThat(applicationStartedGague.getId().getTags()).isEmpty();
	}

	@Test
	void metricRecordedWithoutMainAppClassTagAndAdditionalTags() {
		SpringApplication application = mock(SpringApplication.class);
		Tags tags = Tags.of("foo", "bar");
		this.metrics = new StartupTimeMetrics(this.registry, tags, "started", "ready");
		this.metrics.onApplicationEvent(new ApplicationReadyEvent(application, null, null, Duration.ofSeconds(2)));
		TimeGauge applicationReadyGague = this.registry.find("ready").timeGauge();
		assertThat(applicationReadyGague).isNotNull();
		assertThat(applicationReadyGague.getId().getTags()).containsExactlyElementsOf(tags);
	}

	@Test
	void metricsNotRecordedWhenStartupTimeNotAvailable() {
		this.metrics.onApplicationEvent(applicationStartedEvent(null));
		this.metrics.onApplicationEvent(applicationReadyEvent(null));
		assertThat(this.registry.find("application.started.time").timeGauge()).isNull();
		assertThat(this.registry.find("application.ready.time").timeGauge()).isNull();
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

	private void assertMetricExistsWithValue(String metricName, long expectedValueInMillis) {
		assertMetricExistsWithCustomTagsAndValue(metricName, Tags.empty(), expectedValueInMillis);
	}

	private void assertMetricExistsWithCustomTagsAndValue(String metricName, Tags expectedCustomTags,
			Long expectedValueInMillis) {
		assertThat(this.registry.find(metricName)
				.tags(Tags.concat(expectedCustomTags, "main-application-class", TestMainApplication.class.getName()))
				.timeGauge()).isNotNull().extracting((m) -> m.value(TimeUnit.MILLISECONDS))
						.isEqualTo(expectedValueInMillis.doubleValue());
	}

	static class TestMainApplication {

	}

}
