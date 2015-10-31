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

import java.io.Flushable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.writer.CompositeMetricWriter;
import org.springframework.boot.actuate.metrics.writer.CounterWriter;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.GaugeWriter;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link Exporter} that "exports" by copying metric data from a source
 * {@link MetricReader} to a destination {@link MetricWriter}. Actually the output writer
 * can be a {@link GaugeWriter}, in which case all metrics are simply output as their
 * current value. If the output writer is also a {@link CounterWriter} then metrics whose
 * names begin with "counter." are special: instead of writing them out as simple gauges
 * the writer will increment the counter value. This involves the exporter storing the
 * previous value of the counter so the delta can be computed. For best results with the
 * counters, do not use the exporter concurrently in multiple threads (normally it will
 * only be used periodically and sequentially, even if it is in a background thread, and
 * this is fine).
 *
 * @author Dave Syer
 * @since 1.3.0
 */
public class MetricCopyExporter extends AbstractMetricExporter {

	private static final Log logger = LogFactory.getLog(MetricCopyExporter.class);

	private final MetricReader reader;

	private final GaugeWriter writer;

	private final CounterWriter counter;

	private ConcurrentMap<String, Long> counts = new ConcurrentHashMap<String, Long>();

	private String[] includes = new String[0];

	private String[] excludes = new String[0];

	/**
	 * Create a new {@link MetricCopyExporter} instance.
	 * @param reader the metric reader
	 * @param writer the metric writer
	 */
	public MetricCopyExporter(MetricReader reader, GaugeWriter writer) {
		this(reader, writer, "");
	}

	/**
	 * Create a new {@link MetricCopyExporter} instance.
	 * @param reader the metric reader
	 * @param writer the metric writer
	 * @param prefix the name prefix
	 */
	public MetricCopyExporter(MetricReader reader, GaugeWriter writer, String prefix) {
		super(prefix);
		this.reader = reader;
		this.writer = writer;
		if (writer instanceof CounterWriter) {
			this.counter = (CounterWriter) writer;
		}
		else {
			this.counter = null;
		}
	}

	/**
	 * Set the include patterns used to filter metrics.
	 * @param includes the include patterns
	 */
	public void setIncludes(String... includes) {
		if (includes != null) {
			this.includes = includes;
		}
	}

	/**
	 * Set the exclude patterns used to filter metrics.
	 * @param excludes the exclude patterns
	 */
	public void setExcludes(String... excludes) {
		if (excludes != null) {
			this.excludes = excludes;
		}
	}

	@Override
	protected Iterable<Metric<?>> next(String group) {
		if (ObjectUtils.isEmpty(this.includes) && ObjectUtils.isEmpty(this.excludes)) {
			return this.reader.findAll();
		}
		return new PatternMatchingIterable(MetricCopyExporter.this.reader);
	}

	@Override
	protected void write(String group, Collection<Metric<?>> values) {
		for (Metric<?> value : values) {
			if (value.getName().startsWith("counter.") && this.counter != null) {
				this.counter.increment(calculateDelta(value));
			}
			else {
				this.writer.set(value);
			}
		}
	}

	private Delta<?> calculateDelta(Metric<?> value) {
		long delta = value.getValue().longValue();
		Long old = this.counts.replace(value.getName(), delta);
		if (old != null) {
			delta = delta - old;
		}
		else {
			this.counts.putIfAbsent(value.getName(), delta);
		}
		return new Delta<Long>(value.getName(), delta, value.getTimestamp());
	}

	@Override
	public void flush() {
		flush(this.writer);
	}

	private void flush(GaugeWriter writer) {
		if (writer instanceof CompositeMetricWriter) {
			for (MetricWriter child : (CompositeMetricWriter) writer) {
				flush(child);
			}
		}
		try {
			if (ClassUtils.isPresent("java.io.Flushable", null)) {
				if (writer instanceof Flushable) {
					((Flushable) writer).flush();
					return;
				}
			}
			Method method = ReflectionUtils.findMethod(writer.getClass(), "flush");
			if (method != null) {
				ReflectionUtils.invokeMethod(method, writer);
			}
		}
		catch (Exception ex) {
			logger.warn("Could not flush MetricWriter: " + ex.getClass() + ": "
					+ ex.getMessage());
		}
	}

	private class PatternMatchingIterable implements Iterable<Metric<?>> {

		private final MetricReader reader;

		PatternMatchingIterable(MetricReader reader) {
			this.reader = reader;
		}

		@Override
		public Iterator<Metric<?>> iterator() {
			return new PatternMatchingIterator(this.reader.findAll().iterator());
		}

	}

	private class PatternMatchingIterator implements Iterator<Metric<?>> {

		private Metric<?> buffer = null;

		private Iterator<Metric<?>> iterator;

		PatternMatchingIterator(Iterator<Metric<?>> iterator) {
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
			while (this.iterator.hasNext()) {
				Metric<?> metric = this.iterator.next();
				if (isMatch(metric)) {
					return metric;
				}
			}
			return null;
		}

		private boolean isMatch(Metric<?> metric) {
			String[] includes = MetricCopyExporter.this.includes;
			String[] excludes = MetricCopyExporter.this.excludes;
			String name = metric.getName();
			if (ObjectUtils.isEmpty(includes)
					|| PatternMatchUtils.simpleMatch(includes, name)) {
				return !PatternMatchUtils.simpleMatch(excludes, name);
			}
			return false;
		}

		@Override
		public Metric<?> next() {
			Metric<?> metric = this.buffer;
			this.buffer = null;
			return metric;
		}

	};
}
