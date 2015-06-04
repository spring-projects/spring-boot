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

package org.springframework.boot.actuate.metrics.aggregate;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;
import org.springframework.util.StringUtils;

/**
 * A metric reader that aggregates values from a source reader, normally one that has been
 * collecting data from many sources in the same form (like a scaled-out application). The
 * source has metrics with names in the form <code>*.*.counter.**</code> and
 * <code>*.*.[anything].**</code> (the length of the prefix is controlled by the
 * {@link #setTruncateKeyLength(int) truncateKeyLength} property, and defaults to 2,
 * meaning 2 period separated fields), and the result has metric names in the form
 * <code>aggregate.count.**</code> and <code>aggregate.[anything].**</code>. Counters are
 * summed and anything else (i.e. gauges) are aggregated by choosing the most recent
 * value.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
public class AggregateMetricReader implements MetricReader {

	private MetricReader source;

	private int truncate = 2;

	private String prefix = "aggregate.";

	public AggregateMetricReader(MetricReader source) {
		this.source = source;
	}

	/**
	 * The number of period-separated keys to remove from the start of the input metric
	 * names before aggregating.
	 * @param truncate length of source metric prefixes
	 */
	public void setTruncateKeyLength(int truncate) {
		this.truncate = truncate;
	}

	/**
	 * Prefix to apply to all output metrics. A period will be appended if no present in
	 * the provided value.
	 * @param prefix the prefix to use default "aggregator.")
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix.endsWith(".") ? prefix : prefix + ".";
	}

	@Override
	public Metric<?> findOne(String metricName) {
		if (!metricName.startsWith(this.prefix)) {
			return null;
		}
		InMemoryMetricRepository result = new InMemoryMetricRepository();
		String baseName = metricName.substring(this.prefix.length());
		for (Metric<?> metric : this.source.findAll()) {
			String name = getSourceKey(metric.getName());
			if (baseName.equals(name)) {
				update(result, name, metric);
			}
		}
		return result.findOne(metricName);
	}

	@Override
	public Iterable<Metric<?>> findAll() {
		InMemoryMetricRepository result = new InMemoryMetricRepository();
		for (Metric<?> metric : this.source.findAll()) {
			String key = getSourceKey(metric.getName());
			if (key != null) {
				update(result, key, metric);
			}
		}
		return result.findAll();
	}

	@Override
	public long count() {
		Set<String> names = new HashSet<String>();
		for (Metric<?> metric : this.source.findAll()) {
			String name = getSourceKey(metric.getName());
			if (name != null) {
				names.add(name);
			}
		}
		return names.size();
	}

	private void update(InMemoryMetricRepository result, String key, Metric<?> metric) {
		String name = this.prefix + key;
		Metric<?> aggregate = result.findOne(name);
		if (aggregate == null) {
			aggregate = new Metric<Number>(name, metric.getValue(), metric.getTimestamp());
		}
		else if (key.contains("counter.")) {
			// accumulate all values
			aggregate = new Metric<Number>(name, metric.increment(
					aggregate.getValue().intValue()).getValue(), metric.getTimestamp());
		}
		else if (aggregate.getTimestamp().before(metric.getTimestamp())) {
			// sort by timestamp and only take the latest
			aggregate = new Metric<Number>(name, metric.getValue(), metric.getTimestamp());
		}
		result.set(aggregate);
	}

	private String getSourceKey(String name) {
		String[] keys = StringUtils.delimitedListToStringArray(name, ".");
		if (keys.length <= this.truncate) {
			return null;
		}
		StringBuilder builder = new StringBuilder(keys[this.truncate]);
		for (int i = this.truncate + 1; i < keys.length; i++) {
			builder.append(".").append(keys[i]);
		}
		return builder.toString();
	}

}
