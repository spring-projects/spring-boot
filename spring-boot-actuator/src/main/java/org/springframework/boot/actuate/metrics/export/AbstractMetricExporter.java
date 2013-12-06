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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.util.StringUtils;

/**
 * Base class for metric exporters that have common features, principally a prefix for
 * exported metrics and filtering by timestamp (so only new values are included in the
 * export).
 * 
 * @author Dave Syer
 */
public abstract class AbstractMetricExporter implements Exporter {

	private volatile AtomicBoolean processing = new AtomicBoolean(false);

	private Date earliestTimestamp = new Date();

	private boolean ignoreTimestamps = false;

	private final String prefix;

	public AbstractMetricExporter(String prefix) {
		this.prefix = !StringUtils.hasText(prefix) ? "" : (prefix.endsWith(".") ? prefix
				: prefix + ".");
	}

	/**
	 * The earliest time for which data will be exported.
	 * 
	 * @param earliestTimestamp the timestamp to set
	 */
	public void setEarliestTimestamp(Date earliestTimestamp) {
		this.earliestTimestamp = earliestTimestamp;
	}

	/**
	 * Ignore timestamps (export all metrics).
	 * 
	 * @param ignoreTimestamps the flag to set
	 */
	public void setIgnoreTimestamps(boolean ignoreTimestamps) {
		this.ignoreTimestamps = ignoreTimestamps;
	}

	@Override
	public void export() {
		if (!this.processing.compareAndSet(false, true)) {
			// skip a tick
			return;
		}
		try {
			for (String group : groups()) {
				Collection<Metric<?>> values = new ArrayList<Metric<?>>();
				for (Metric<?> metric : next(group)) {
					Metric<?> value = new Metric<Number>(this.prefix + metric.getName(),
							metric.getValue(), metric.getTimestamp());
					Date timestamp = metric.getTimestamp();
					if (!this.ignoreTimestamps && this.earliestTimestamp.after(timestamp)) {
						continue;
					}
					values.add(value);
				}
				write(group, values);
			}
		}
		finally {
			this.processing.set(false);
		}
	}

	/**
	 * Generate a group of metrics to iterate over in the form of a set of Strings (e.g.
	 * prefixes). If the metrics to be exported partition into groups identified by a
	 * String, subclasses should override this method. Otherwise the default should be
	 * fine (iteration over all metrics).
	 * 
	 * @return groups of metrics to iterate over (default singleton empty string)
	 */
	protected Iterable<String> groups() {
		return Collections.singleton("");
	}

	/**
	 * Write the values associated with a group.
	 * 
	 * @param group the group to write
	 * @param values the values to write
	 */
	protected abstract void write(String group, Collection<Metric<?>> values);

	/**
	 * Get the next group of metrics to write.
	 * 
	 * @param group the group name to write
	 * @return some metrics to write
	 */
	protected abstract Iterable<Metric<?>> next(String group);

}