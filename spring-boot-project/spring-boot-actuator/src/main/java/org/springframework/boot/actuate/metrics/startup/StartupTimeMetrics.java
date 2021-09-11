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
 * @since 2.6.0
 */
public class StartupTimeMetrics implements SmartApplicationListener {

	private final MeterRegistry meterRegistry;

	private final RuntimeMXBean runtimeMXBean;

	private final String applicationStartedTimeMetricName;

	private final String applicationReadyTimeMetricName;

	private final String applicationReadyJvmTimeMetricName;

	private final Iterable<Tag> tags;

	public StartupTimeMetrics(MeterRegistry meterRegistry, RuntimeMXBean runtimeMXBean) {
		this(meterRegistry, runtimeMXBean, Collections.emptyList(), "application.started.time",
				"application.ready.time", "application.ready.jvm.time");
	}

	public StartupTimeMetrics(MeterRegistry meterRegistry, RuntimeMXBean runtimeMXBean, Iterable<Tag> tags,
			String applicationStartedTimeMetricName, String applicationReadyTimeMetricName,
			String applicationReadyJvmTimeMetricName) {
		this.meterRegistry = meterRegistry;
		this.runtimeMXBean = runtimeMXBean;
		this.tags = (tags != null) ? tags : Collections.emptyList();
		this.applicationStartedTimeMetricName = applicationStartedTimeMetricName;
		this.applicationReadyTimeMetricName = applicationReadyTimeMetricName;
		this.applicationReadyJvmTimeMetricName = applicationReadyJvmTimeMetricName;
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return ApplicationStartedEvent.class.isAssignableFrom(eventType)
				|| ApplicationReadyEvent.class.isAssignableFrom(eventType);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationStartedEvent) {
			onApplicationStarted((ApplicationStartedEvent) event);
		}
		if (event instanceof ApplicationReadyEvent) {
			onApplicationReady((ApplicationReadyEvent) event);
		}
	}

	private void onApplicationStarted(ApplicationStartedEvent event) {
		if (event.getStartupTime() == null) {
			return;
		}
		TimeGauge
				.builder(this.applicationStartedTimeMetricName, () -> event.getStartupTime().toMillis(),
						TimeUnit.MILLISECONDS)
				.tags(maybeDcorateTagsWithApplicationInfo(event.getSpringApplication()))
				.description("Time taken (ms) to start the application").register(this.meterRegistry);
	}

	private void onApplicationReady(ApplicationReadyEvent event) {
		if (event.getStartupTime() == null) {
			return;
		}
		TimeGauge
				.builder(this.applicationReadyTimeMetricName, () -> event.getStartupTime().toMillis(),
						TimeUnit.MILLISECONDS)
				.tags(maybeDcorateTagsWithApplicationInfo(event.getSpringApplication()))
				.description("Time taken (ms) for the application to be ready to serve requests")
				.register(this.meterRegistry);
		TimeGauge
				.builder(this.applicationReadyJvmTimeMetricName, () -> this.runtimeMXBean.getUptime(),
						TimeUnit.MILLISECONDS)
				.tags(maybeDcorateTagsWithApplicationInfo(event.getSpringApplication()))
				.description("The uptime of the JVM (ms) when the application is ready to serve requests")
				.register(this.meterRegistry);
	}

	private Iterable<Tag> maybeDcorateTagsWithApplicationInfo(SpringApplication springApplication) {
		Class<?> mainClass = springApplication.getMainApplicationClass();
		if (mainClass == null) {
			return this.tags;
		}
		return Tags.concat(this.tags, "main-application-class", mainClass.getName());
	}

}
