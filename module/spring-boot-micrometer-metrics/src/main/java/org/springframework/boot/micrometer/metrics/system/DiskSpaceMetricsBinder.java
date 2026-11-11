/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.system;

import java.io.File;
import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;

import org.springframework.util.Assert;

/**
 * A {@link MeterBinder} that binds one or more {@link DiskSpaceMetrics}.
 *
 * @author Chris Bono
 * @since 4.0.0
 */
public class DiskSpaceMetricsBinder implements MeterBinder {

	private final List<File> paths;

	private final Iterable<Tag> tags;

	public DiskSpaceMetricsBinder(List<File> paths, Iterable<Tag> tags) {
		Assert.notEmpty(paths, "'paths' must not be empty");
		this.paths = paths;
		this.tags = tags;
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		this.paths.forEach((path) -> new DiskSpaceMetrics(path, this.tags).bindTo(registry));
	}

}
