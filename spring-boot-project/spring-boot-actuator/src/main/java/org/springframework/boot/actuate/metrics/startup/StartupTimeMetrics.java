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

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;

/**
 * Binds application startup metrics in response to an {@link ApplicationStartedEvent}.
 *
 * @author Chris Bono
 * @since 2.6.0
 */
public class StartupTimeMetrics implements ApplicationListener<ApplicationStartedEvent> {

	private final MeterRegistry meterRegistry;

	private final Iterable<Tag> tags;

	public StartupTimeMetrics(MeterRegistry meterRegistry) {
		this(meterRegistry, Collections.emptyList());
	}

	public StartupTimeMetrics(MeterRegistry meterRegistry, Iterable<Tag> tags) {
		this.meterRegistry = meterRegistry;
		this.tags = (tags != null) ? tags : Collections.emptyList();
	}

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		if (event.getStartupTime() == null) {
			return;
		}
		TimeGauge
				.builder("spring.boot.application.started", () -> event.getStartupTime().toMillis(),
						TimeUnit.MILLISECONDS)
				.tags(maybeDcorateTagsWithEventInfo(event)).description("Application startup time in milliseconds")
				.register(this.meterRegistry);
	}

	private Iterable<Tag> maybeDcorateTagsWithEventInfo(ApplicationStartedEvent event) {
		Class<?> mainClass = event.getSpringApplication().getMainApplicationClass();
		if (mainClass == null) {
			return this.tags;
		}
		return Tags.concat(this.tags, "main-application-class", mainClass.getName());
	}

}
