/*
 * Copyright 2012-2015 the original author or authors.
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;

/**
 * A {@link MetricReader} that pulls all current values out of the {@link MetricsEndpoint}
 * . No timestamp information is available, so there is no way to check if the values are
 * recent, and they all come out with the default (current time).
 * 
 * @author Dave Syer
 *
 */
public class MetricsEndpointMetricReader implements MetricReader {

	private final MetricsEndpoint endpoint;

	public MetricsEndpointMetricReader(MetricsEndpoint endpoint) {
		this.endpoint = endpoint;
	}

	@Override
	public Metric<?> findOne(String metricName) {
		Metric<Number> metric = null;
		Object value = endpoint.invoke().get(metricName);
		if (value != null) {
			metric = new Metric<Number>(metricName, (Number) value);
		}
		return metric;
	}

	@Override
	public Iterable<Metric<?>> findAll() {
		List<Metric<?>> metrics = new ArrayList<Metric<?>>();
		Map<String, Object> values = endpoint.invoke();
		Date timestamp = new Date();
		for (Entry<String, Object> entry : values.entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();
			metrics.add(new Metric<Number>(name, (Number) value, timestamp));
		}
		return metrics;
	}

	@Override
	public long count() {
		return endpoint.invoke().size();
	}

}
