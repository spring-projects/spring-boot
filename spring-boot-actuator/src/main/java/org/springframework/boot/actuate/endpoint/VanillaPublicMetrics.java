/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link PublicMetrics} that exposes all metrics from a
 * {@link MetricReader} along with a collection of configurable {@link PublicMetrics}
 * instances.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Stephane Nicoll
 */
public class VanillaPublicMetrics implements PublicMetrics {

	private final MetricReader reader;
	private final Collection<PublicMetrics> publicMetrics;

	public VanillaPublicMetrics(MetricReader reader, Collection<PublicMetrics> publicMetrics) {
		Assert.notNull(reader, "MetricReader must not be null");
		Assert.notNull(publicMetrics, "PublicMetrics must not be null");
		this.reader = reader;
		this.publicMetrics = publicMetrics;
	}

	public VanillaPublicMetrics(MetricReader reader) {
		this(reader, Collections.<PublicMetrics>emptyList());
	}

	@Override
	public Collection<Metric<?>> metrics() {
		Collection<Metric<?>> result = new LinkedHashSet<Metric<?>>();
		for (Metric<?> metric : this.reader.findAll()) {
			result.add(metric);
		}
		for (PublicMetrics publicMetric : publicMetrics) {
			result.addAll(publicMetric.metrics());
		}

		return result;
	}

}
