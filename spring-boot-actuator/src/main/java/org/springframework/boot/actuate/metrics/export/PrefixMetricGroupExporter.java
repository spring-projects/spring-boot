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

package org.springframework.boot.actuate.metrics.export;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.PrefixMetricReader;
import org.springframework.boot.actuate.metrics.repository.MultiMetricRepository;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;

/**
 * @author Dave Syer
 */
public class PrefixMetricGroupExporter extends AbstractMetricExporter {

	private final PrefixMetricReader reader;
	private final MetricWriter writer;
	private Set<String> groups = new HashSet<String>();

	public PrefixMetricGroupExporter(PrefixMetricReader reader, MetricWriter writer) {
		this(reader, writer, "");
	}

	public PrefixMetricGroupExporter(PrefixMetricReader reader, MetricWriter writer,
			String prefix) {
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
	protected Iterable<Metric<?>> next(String group) {
		return this.reader.findAll(group);
	}

	@Override
	protected Iterable<String> groups() {
		return this.groups;
	}

	@Override
	protected void write(String group, Collection<Metric<?>> values) {
		if (this.writer instanceof MultiMetricRepository && !values.isEmpty()) {
			((MultiMetricRepository) this.writer).save(group, values);
		}
		else {
			for (Metric<?> value : values) {
				this.writer.set(value);
			}
		}
	}

}
