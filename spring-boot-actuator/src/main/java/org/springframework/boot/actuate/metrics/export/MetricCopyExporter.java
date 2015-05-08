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

package org.springframework.boot.actuate.metrics.export;

import java.util.Collection;
import java.util.Iterator;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.actuate.metrics.writer.WriterUtils;
import org.springframework.util.PatternMatchUtils;

/**
 * {@link Exporter} that "exports" by copying metric data from a source
 * {@link MetricReader} to a destination {@link MetricWriter}.
 *
 * @author Dave Syer
 */
public class MetricCopyExporter extends AbstractMetricExporter {

	private final MetricReader reader;

	private final MetricWriter writer;

	private String[] includes = new String[0];

	private String[] excludes = new String[0];

	public MetricCopyExporter(MetricReader reader, MetricWriter writer) {
		this(reader, writer, "");
	}

	public void setIncludes(String... includes) {
		if (includes != null) {
			this.includes = includes;
		}
	}

	public void setExcludes(String... excludes) {
		if (excludes != null) {
			this.excludes = excludes;
		}
	}

	public MetricCopyExporter(MetricReader reader, MetricWriter writer, String prefix) {
		super(prefix);
		this.reader = reader;
		this.writer = writer;
	}

	@Override
	protected Iterable<Metric<?>> next(String group) {
		if ((this.includes == null || this.includes.length == 0)
				&& (this.excludes == null || this.excludes.length == 0)) {
			return this.reader.findAll();
		}
		return new Iterable<Metric<?>>() {
			@Override
			public Iterator<Metric<?>> iterator() {
				return new PatternMatchingIterator(MetricCopyExporter.this.reader
						.findAll().iterator());
			}
		};
	}

	@Override
	protected void write(String group, Collection<Metric<?>> values) {
		for (Metric<?> value : values) {
			this.writer.set(value);
		}
	}

	@Override
	public void flush() {
		WriterUtils.flush(this.writer);
	}

	private class PatternMatchingIterator implements Iterator<Metric<?>> {

		private Metric<?> buffer = null;
		private Iterator<Metric<?>> iterator;

		public PatternMatchingIterator(Iterator<Metric<?>> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {
			if (this.buffer != null) {
				return true;
			}
			this.buffer = findNext();
			return this.buffer != null;
		}

		private Metric<?> findNext() {
			Metric<?> metric = null;
			boolean matched = false;
			while (this.iterator.hasNext() && !matched) {
				metric = this.iterator.next();
				if (MetricCopyExporter.this.includes == null
						|| MetricCopyExporter.this.includes.length == 0) {
					matched = true;
				}
				else {
					for (String pattern : MetricCopyExporter.this.includes) {
						if (PatternMatchUtils.simpleMatch(pattern, metric.getName())) {
							matched = true;
							break;
						}
					}
				}
				if (MetricCopyExporter.this.excludes != null) {
					for (String pattern : MetricCopyExporter.this.excludes) {
						if (PatternMatchUtils.simpleMatch(pattern, metric.getName())) {
							matched = false;
							break;
						}
					}
				}
			}
			return matched ? metric : null;

		}

		@Override
		public Metric<?> next() {
			Metric<?> metric = this.buffer;
			this.buffer = null;
			return metric;
		}
	};
}
