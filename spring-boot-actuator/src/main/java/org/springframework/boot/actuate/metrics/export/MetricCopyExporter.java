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

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;

/**
 * {@link Exporter} that "exports" by copying metric data from a source
 * {@link MetricReader} to a destination {@link MetricWriter}.
 * 
 * @author Dave Syer
 */
public class MetricCopyExporter extends AbstractMetricExporter {

	private final MetricReader reader;

	private final MetricWriter writer;

	public MetricCopyExporter(MetricReader reader, MetricWriter writer) {
		this(reader, writer, "");
	}

	public MetricCopyExporter(MetricReader reader, MetricWriter writer, String prefix) {
		super(prefix);
		this.reader = reader;
		this.writer = writer;
	}

	@Override
	protected Iterable<Metric<?>> next(String group) {
		return this.reader.findAll();
	}

	@Override
	protected void write(String group, Collection<Metric<?>> values) {
		for (Metric<?> value : values) {
			this.writer.set(value);
		}
	}

}
