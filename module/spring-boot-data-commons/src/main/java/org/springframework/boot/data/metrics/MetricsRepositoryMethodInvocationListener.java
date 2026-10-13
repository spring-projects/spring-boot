/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.metrics;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.jspecify.annotations.Nullable;

import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener;
import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;

/**
 * Intercepts Spring Data {@code Repository} invocations and records metrics about
 * execution time and results.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class MetricsRepositoryMethodInvocationListener implements RepositoryMethodInvocationListener {

	private final SingletonSupplier<MeterRegistry> registrySupplier;

	private final RepositoryTagsProvider tagsProvider;

	private final String metricName;

	private final AutoTimer autoTimer;

	/**
	 * Create a new {@code MetricsRepositoryMethodInvocationListener}.
	 * @param registrySupplier a supplier for the registry to which metrics are recorded
	 * @param tagsProvider provider for metrics tags
	 * @param metricName name of the metric to record
	 * @param autoTimer the auto-timers to apply or {@code null} to disable auto-timing
	 * @since 4.0.0
	 */
	public MetricsRepositoryMethodInvocationListener(Supplier<MeterRegistry> registrySupplier,
			RepositoryTagsProvider tagsProvider, String metricName, @Nullable AutoTimer autoTimer) {
		this.registrySupplier = (registrySupplier instanceof SingletonSupplier)
				? (SingletonSupplier<MeterRegistry>) registrySupplier : SingletonSupplier.of(registrySupplier);
		this.tagsProvider = tagsProvider;
		this.metricName = metricName;
		this.autoTimer = (autoTimer != null) ? autoTimer : AutoTimer.DISABLED;
	}

	@Override
	public void afterInvocation(RepositoryMethodInvocation invocation) {
		Set<Timed> annotations = TimedAnnotations.get(invocation.getMethod(), invocation.getRepositoryInterface());
		Iterable<Tag> tags = this.tagsProvider.repositoryTags(invocation);
		long duration = invocation.getDuration(TimeUnit.NANOSECONDS);
		AutoTimer.apply(this.autoTimer, this.metricName, annotations, (builder) -> {
			MeterRegistry registry = this.registrySupplier.get();
			Assert.state(registry != null, "'registry' must not be null");
			builder.description("Duration of repository invocations")
				.tags(tags)
				.register(registry)
				.record(duration, TimeUnit.NANOSECONDS);
		});
	}

}
