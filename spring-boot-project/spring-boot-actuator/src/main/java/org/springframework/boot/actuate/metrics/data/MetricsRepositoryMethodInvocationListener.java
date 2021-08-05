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

package org.springframework.boot.actuate.metrics.data;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.boot.actuate.metrics.annotation.TimedAnnotations;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener;

/**
 * Intercepts Spring Data {@code Repository} invocations and records metrics about
 * execution time and results.
 *
 * @author Phillip Webb
 * @since 2.5.0
 */
public class MetricsRepositoryMethodInvocationListener implements RepositoryMethodInvocationListener {

	private final MeterRegistry registry;

	private final RepositoryTagsProvider tagsProvider;

	private final String metricName;

	private final AutoTimer autoTimer;

	/**
	 * Create a new {@code MetricsRepositoryMethodInvocationListener}.
	 * @param registry the registry to which metrics are recorded
	 * @param tagsProvider provider for metrics tags
	 * @param metricName name of the metric to record
	 * @param autoTimer the auto-timers to apply or {@code null} to disable auto-timing
	 */
	public MetricsRepositoryMethodInvocationListener(MeterRegistry registry, RepositoryTagsProvider tagsProvider,
			String metricName, AutoTimer autoTimer) {
		this.registry = registry;
		this.tagsProvider = tagsProvider;
		this.metricName = metricName;
		this.autoTimer = (autoTimer != null) ? autoTimer : AutoTimer.DISABLED;
	}

	@Override
	public void afterInvocation(RepositoryMethodInvocation invocation) {
		Set<Timed> annotations = TimedAnnotations.get(invocation.getMethod(), invocation.getRepositoryInterface());
		Iterable<Tag> tags = this.tagsProvider.repositoryTags(invocation);
		long duration = invocation.getDuration(TimeUnit.NANOSECONDS);
		AutoTimer.apply(this.autoTimer, this.metricName, annotations,
				(builder) -> builder.tags(tags).register(this.registry).record(duration, TimeUnit.NANOSECONDS));
	}

}
