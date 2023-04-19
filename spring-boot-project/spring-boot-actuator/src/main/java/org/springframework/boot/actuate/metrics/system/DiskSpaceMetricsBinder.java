/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.metrics.system;

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
 * @since 2.6.0
 */
public class DiskSpaceMetricsBinder implements MeterBinder {

	private final List<File> paths;

	private final Iterable<Tag> tags;

	public DiskSpaceMetricsBinder(List<File> paths, Iterable<Tag> tags) {
		Assert.notEmpty(paths, "Paths must not be empty");
		this.paths = paths;
		this.tags = tags;
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		this.paths.forEach((path) -> new DiskSpaceMetrics(path, this.tags).bindTo(registry));
	}

}
