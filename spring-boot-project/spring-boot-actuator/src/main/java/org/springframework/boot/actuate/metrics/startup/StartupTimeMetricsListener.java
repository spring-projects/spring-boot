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
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;

/**
 * Binds application startup metrics in response to {@link ApplicationStartedEvent} and
 * {@link ApplicationReadyEvent}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 * @since 2.6.0
 */
public class StartupTimeMetricsListener implements SmartApplicationListener {

	/**
	 * The default name to use for the application started time metric.
	 */
	public static final String APPLICATION_STARTED_TIME_METRIC_NAME = "application.started.time";

	/**
	 * The default name to use for the application ready time metric.
	 */
	public static final String APPLICATION_READY_TIME_METRIC_NAME = "application.ready.time";

	private final MeterRegistry meterRegistry;

	private final String startedTimeMetricName;

	private final String readyTimeMetricName;

	private final Tags tags;

	/**
	 * Create a new instance using default metric names.
	 * @param meterRegistry the registry to use
	 * @see #APPLICATION_STARTED_TIME_METRIC_NAME
	 * @see #APPLICATION_READY_TIME_METRIC_NAME
	 */
	public StartupTimeMetricsListener(MeterRegistry meterRegistry) {
		this(meterRegistry, APPLICATION_STARTED_TIME_METRIC_NAME, APPLICATION_READY_TIME_METRIC_NAME,
				Collections.emptyList());
	}

	/**
	 * Create a new instance using the specified options.
	 * @param meterRegistry the registry to use
	 * @param startedTimeMetricName the name to use for the application started time
	 * metric
	 * @param readyTimeMetricName the name to use for the application ready time metric
	 * @param tags the tags to associate to application startup metrics
	 */
	public StartupTimeMetricsListener(MeterRegistry meterRegistry, String startedTimeMetricName,
			String readyTimeMetricName, Iterable<Tag> tags) {
		this.meterRegistry = meterRegistry;
		this.startedTimeMetricName = startedTimeMetricName;
		this.readyTimeMetricName = readyTimeMetricName;
		this.tags = Tags.of(tags);
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return ApplicationStartedEvent.class.isAssignableFrom(eventType)
				|| ApplicationReadyEvent.class.isAssignableFrom(eventType);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationStartedEvent startedEvent) {
			onApplicationStarted(startedEvent);
		}
		if (event instanceof ApplicationReadyEvent readyEvent) {
			onApplicationReady(readyEvent);
		}
	}

	private void onApplicationStarted(ApplicationStartedEvent event) {
		registerGauge(this.startedTimeMetricName, "Time taken to start the application", event.getTimeTaken(),
				event.getSpringApplication());
	}

	private void onApplicationReady(ApplicationReadyEvent event) {
		registerGauge(this.readyTimeMetricName, "Time taken for the application to be ready to service requests",
				event.getTimeTaken(), event.getSpringApplication());
	}

	private void registerGauge(String name, String description, Duration timeTaken,
			SpringApplication springApplication) {
		if (timeTaken != null) {
			Iterable<Tag> tags = createTagsFrom(springApplication);
			TimeGauge.builder(name, timeTaken::toMillis, TimeUnit.MILLISECONDS)
				.tags(tags)
				.description(description)
				.register(this.meterRegistry);
		}
	}

	private Iterable<Tag> createTagsFrom(SpringApplication springApplication) {
		Class<?> mainClass = springApplication.getMainApplicationClass();
		return (mainClass != null) ? this.tags.and("main.application.class", mainClass.getName()) : this.tags;
	}

}
