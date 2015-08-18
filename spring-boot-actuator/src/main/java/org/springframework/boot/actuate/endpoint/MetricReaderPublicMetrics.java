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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.util.Assert;

/**
 * {@link PublicMetrics} exposed from a {@link MetricReader}.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public class MetricReaderPublicMetrics implements PublicMetrics {

	private final MetricReader metricReader;

	public MetricReaderPublicMetrics(MetricReader metricReader) {
		Assert.notNull(metricReader, "MetricReader must not be null");
		this.metricReader = metricReader;
	}

	@Override
	public Collection<Metric<?>> metrics() {
		List<Metric<?>> result = new ArrayList<Metric<?>>();
		for (Metric<?> metric : this.metricReader.findAll()) {
			result.add(metric);
		}
		return result;
	}

}
