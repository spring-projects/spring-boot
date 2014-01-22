/*
 * Copyright 2012-2013 the original author or authors.
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
import java.util.LinkedHashSet;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link PublicMetrics} that exposes all metrics from a
 * {@link MetricReader} along with memory information.
 * 
 * @author Dave Syer
 */
public class VanillaPublicMetrics implements PublicMetrics {

	private final MetricReader reader;

	public VanillaPublicMetrics(MetricReader reader) {
		Assert.notNull(reader, "MetricReader must not be null");
		this.reader = reader;
	}

	@Override
	public Collection<Metric<?>> metrics() {
		Collection<Metric<?>> result = new LinkedHashSet<Metric<?>>();
		for (Metric<?> metric : this.reader.findAll()) {
			result.add(metric);
		}
		result.add(new Metric<Long>("mem",
				new Long(Runtime.getRuntime().totalMemory()) / 1024));
		result.add(new Metric<Long>("mem.free", new Long(Runtime.getRuntime()
				.freeMemory()) / 1024));
		result.add(new Metric<Integer>("processors", Runtime.getRuntime()
				.availableProcessors()));
		return result;
	}

}
