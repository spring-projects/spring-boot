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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.util.StringUtils;

/**
 * Base class for metric exporters that have common features, principally a prefix for
 * exported metrics and filtering by timestamp (so only new values are included in the
 * export).
 *
 * @author Dave Syer
 * @since 1.3.0
 */
public abstract class AbstractMetricExporter implements Exporter {

	private static final Log logger = LogFactory.getLog(AbstractMetricExporter.class);

	private final String prefix;

	private Date earliestTimestamp = new Date();

	private boolean ignoreTimestamps = false;

	private boolean sendLatest = true;

	private volatile AtomicBoolean processing = new AtomicBoolean(false);

	private Date latestTimestamp = new Date(0L);

	public AbstractMetricExporter(String prefix) {
		this.prefix = (!StringUtils.hasText(prefix) ? ""
				: (prefix.endsWith(".") ? prefix : prefix + "."));
	}

	/**
	 * The earliest time for which data will be exported.
	 * @param earliestTimestamp the timestamp to set
	 */
	public void setEarliestTimestamp(Date earliestTimestamp) {
		this.earliestTimestamp = earliestTimestamp;
	}

	/**
	 * Ignore timestamps (export all metrics).
	 * @param ignoreTimestamps the flag to set
	 */
	public void setIgnoreTimestamps(boolean ignoreTimestamps) {
		this.ignoreTimestamps = ignoreTimestamps;
	}

	/**
	 * Send only the data that changed since the last export.
	 * @param sendLatest the flag to set
	 */
	public void setSendLatest(boolean sendLatest) {
		this.sendLatest = sendLatest;
	}

	@Override
	public void export() {
		if (this.processing.compareAndSet(false, true)) {
			long latestTimestamp = System.currentTimeMillis();
			try {
				exportGroups();
			}
			catch (Exception ex) {
				logger.warn("Could not write to MetricWriter: " + ex.getClass() + ": "
						+ ex.getMessage());
			}
			finally {
				this.latestTimestamp = new Date(latestTimestamp);
				flushQuietly();
				this.processing.set(false);
			}
		}
	}

	private void exportGroups() {
		for (String group : groups()) {
			Collection<Metric<?>> values = new ArrayList<Metric<?>>();
			for (Metric<?> metric : next(group)) {
				Date timestamp = metric.getTimestamp();
				if (canExportTimestamp(timestamp)) {
					values.add(getPrefixedMetric(metric));
				}
			}
			if (!values.isEmpty()) {
				write(group, values);
			}
		}
	}

	private Metric<?> getPrefixedMetric(Metric<?> metric) {
		String name = this.prefix + metric.getName();
		return new Metric<Number>(name, metric.getValue(), metric.getTimestamp());
	}

	private boolean canExportTimestamp(Date timestamp) {
		if (this.ignoreTimestamps) {
			return true;
		}
		if (this.earliestTimestamp.after(timestamp)) {
			return false;
		}
		if (this.sendLatest && this.latestTimestamp.after(timestamp)) {
			return false;
		}
		return true;
	}

	private void flushQuietly() {
		try {
			flush();
		}
		catch (Exception ex) {
			logger.warn("Could not flush MetricWriter: " + ex.getClass() + ": "
					+ ex.getMessage());
		}
	}

	public void flush() {
	}

	/**
	 * Generate a group of metrics to iterate over in the form of a set of Strings (e.g.
	 * prefixes). If the metrics to be exported partition into groups identified by a
	 * String, subclasses should override this method. Otherwise the default should be
	 * fine (iteration over all metrics).
	 * @return groups of metrics to iterate over (default singleton empty string)
	 */
	protected Iterable<String> groups() {
		return Collections.singleton("");
	}

	/**
	 * Write the values associated with a group.
	 * @param group the group to write
	 * @param values the values to write
	 */
	protected abstract void write(String group, Collection<Metric<?>> values);

	/**
	 * Get the next group of metrics to write.
	 * @param group the group name to write
	 * @return some metrics to write
	 */
	protected abstract Iterable<Metric<?>> next(String group);

}
