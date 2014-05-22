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

package org.springframework.boot.actuate.metrics.export;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.PrefixMetricReader;
import org.springframework.boot.actuate.metrics.repository.MultiMetricRepository;
import org.springframework.boot.actuate.metrics.writer.PrefixMetricWriter;

/**
 * A convenient exporter for a group of metrics from a {@link PrefixMetricReader}. Exports
 * all metrics whose name starts with a prefix (or all metrics if the prefix is empty).
 * 
 * @author Dave Syer
 */
public class PrefixMetricGroupExporter extends AbstractMetricExporter {

	private final PrefixMetricReader reader;

	private final PrefixMetricWriter writer;

	private Set<String> groups = new HashSet<String>();

	/**
	 * Create a new exporter for metrics to a writer based on an empty prefix for the
	 * metric names.
	 * @param reader a reader as the source of metrics
	 * @param writer the writer to send the metrics to
	 */
	public PrefixMetricGroupExporter(PrefixMetricReader reader, PrefixMetricWriter writer) {
		this(reader, writer, "");
	}

	/**
	 * Create a new exporter for metrics to a writer based on a prefix for the metric
	 * names.
	 * @param reader a reader as the source of metrics
	 * @param writer the writer to send the metrics to
	 * @param prefix the prefix for metrics to export
	 */
	public PrefixMetricGroupExporter(PrefixMetricReader reader,
			PrefixMetricWriter writer, String prefix) {
		super(prefix);
		this.reader = reader;
		this.writer = writer;
	}

	/**
	 * @param groups the groups to set
	 */
	public void setGroups(Set<String> groups) {
		this.groups = groups;
	}

	@Override
	protected Iterable<String> groups() {
		if ((this.reader instanceof MultiMetricRepository) && this.groups.isEmpty()) {
			return ((MultiMetricRepository) this.reader).groups();
		}
		return this.groups;
	}

	@Override
	protected Iterable<Metric<?>> next(String group) {
		return this.reader.findAll(group);
	}

	@Override
	protected void write(String group, Collection<Metric<?>> values) {
		this.writer.set(group, values);
	}

}
