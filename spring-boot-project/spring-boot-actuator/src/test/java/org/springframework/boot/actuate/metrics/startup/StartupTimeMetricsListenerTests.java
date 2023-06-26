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

package org.springframework.boot.actuate.metrics.startup;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StartupTimeMetricsListener}.
 *
 * @author Chris Bono
 * @author Michał Rowicki
 */
class StartupTimeMetricsListenerTests {

	private MeterRegistry registry;

	private StartupTimeMetricsListener listener;

	@BeforeEach
	void setup() {
		this.registry = new SimpleMeterRegistry();
		this.listener = new StartupTimeMetricsListener(this.registry);
	}

	@Test
	void metricsRecordedWithoutCustomTags() {
		this.listener.onApplicationEvent(applicationStartedEvent(2000L));
		this.listener.onApplicationEvent(applicationReadyEvent(2200L));
		assertMetricExistsWithValue("application.started.time", 2000L);
		assertMetricExists("process.started.time");
		assertMetricExistsWithValue("application.ready.time", 2200L);
	}

	@Test
	void metricsRecordedWithCustomTagsAndMetricNames() {
		Tags tags = Tags.of("foo", "bar");
		this.listener = new StartupTimeMetricsListener(this.registry, "m1", "m2", "m3", tags);
		this.listener.onApplicationEvent(applicationStartedEvent(1000L));
		this.listener.onApplicationEvent(applicationReadyEvent(1050L));
		assertMetricExistsWithCustomTagsAndValue("m1", tags, 1000L);
		assertMetricExistsWithCustomTags("m2", tags);
		assertMetricExistsWithCustomTagsAndValue("m3", tags, 1050L);
	}

	@Test
	void metricRecordedWithoutMainAppClassTag() {
		SpringApplication application = mock(SpringApplication.class);
		this.listener.onApplicationEvent(new ApplicationStartedEvent(application, null, null, Duration.ofSeconds(2)));
		TimeGauge applicationStartedGauge = this.registry.find("application.started.time").timeGauge();
		TimeGauge processStartedTime = this.registry.find("process.started.time").timeGauge();
		assertThat(applicationStartedGauge).isNotNull();
		assertThat(applicationStartedGauge.getId().getTags()).isEmpty();
		assertThat(processStartedTime).isNotNull();
		assertThat(processStartedTime.getId().getTags()).isEmpty();
	}

	@Test
	void metricRecordedWithoutMainAppClassTagAndAdditionalTags() {
		SpringApplication application = mock(SpringApplication.class);
		Tags tags = Tags.of("foo", "bar");
		this.listener = new StartupTimeMetricsListener(this.registry, "started", "jvm", "ready", tags);
		this.listener.onApplicationEvent(new ApplicationReadyEvent(application, null, null, Duration.ofSeconds(2)));
		TimeGauge applicationReadyGauge = this.registry.find("ready").timeGauge();
		assertThat(applicationReadyGauge).isNotNull();
		assertThat(applicationReadyGauge.getId().getTags()).containsExactlyElementsOf(tags);
	}

	@Test
	void metricsNotRecordedWhenStartupTimeNotAvailable() {
		this.listener.onApplicationEvent(applicationStartedEvent(null));
		this.listener.onApplicationEvent(applicationReadyEvent(null));
		assertThat(this.registry.find("application.started.time").timeGauge()).isNull();
		assertThat(this.registry.find("application.ready.time").timeGauge()).isNull();
	}

	@Test
	void startupJvmMetricIsRecordedEvenWhenStartupTimeIsNotAvailable() {
		this.listener.onApplicationEvent(applicationStartedEvent(null));
		assertThat(this.registry.find("process.started.time").timeGauge()).isNotNull();
	}

	private ApplicationStartedEvent applicationStartedEvent(Long startupTimeMs) {
		SpringApplication application = mock(SpringApplication.class);
		given(application.getMainApplicationClass()).willAnswer((invocation) -> TestMainApplication.class);
		return new ApplicationStartedEvent(application, null, null,
				(startupTimeMs != null) ? Duration.ofMillis(startupTimeMs) : null);
	}

	private ApplicationReadyEvent applicationReadyEvent(Long startupTimeMs) {
		SpringApplication application = mock(SpringApplication.class);
		given(application.getMainApplicationClass()).willAnswer((invocation) -> TestMainApplication.class);
		return new ApplicationReadyEvent(application, null, null,
				(startupTimeMs != null) ? Duration.ofMillis(startupTimeMs) : null);
	}

	private void assertMetricExistsWithValue(String metricName, long expectedValueInMillis) {
		assertMetricExistsWithCustomTagsAndValue(metricName, Tags.empty(), expectedValueInMillis);
	}

	private void assertMetricExistsWithCustomTagsAndValue(String metricName, Tags expectedCustomTags,
			Long expectedValueInMillis) {
		assertThat(this.registry.find(metricName)
			.tags(Tags.concat(expectedCustomTags, "main.application.class", TestMainApplication.class.getName()))
			.timeGauge()).isNotNull()
			.extracting((m) -> m.value(TimeUnit.MILLISECONDS))
			.isEqualTo(expectedValueInMillis.doubleValue());
	}

	private void assertMetricExists(String metricName) {
		assertMetricExistsWithCustomTags(metricName, Tags.empty());
	}

	private void assertMetricExistsWithCustomTags(String metricName, Tags expectedCustomTags) {
		assertThat(this.registry.find(metricName)
			.tags(Tags.concat(expectedCustomTags, "main.application.class", TestMainApplication.class.getName()))
			.timeGauge()).isNotNull()
			.extracting((m) -> m.value(TimeUnit.MILLISECONDS))
			.satisfies(new Condition<>((d) -> d > 0.0, "greater than zero"));
	}

	static class TestMainApplication {

	}

}
