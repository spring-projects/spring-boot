/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.metrics.writer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.boot.actuate.metrics.Metric;

/**
 * Composite implementation of {@link MetricWriter} that just sends its input to all of
 * the delegates that have been registered.
 *
 * @author Dave Syer
 */
public class CompositeMetricWriter implements MetricWriter, Iterable<MetricWriter> {

	private final List<MetricWriter> writers = new ArrayList<MetricWriter>();

	public CompositeMetricWriter(MetricWriter... writers) {
		Collections.addAll(this.writers, writers);
	}

	public CompositeMetricWriter(List<MetricWriter> writers) {
		this.writers.addAll(writers);
	}

	@Override
	public Iterator<MetricWriter> iterator() {
		return this.writers.iterator();
	}

	@Override
	public void increment(Delta<?> delta) {
		for (MetricWriter writer : this.writers) {
			writer.increment(delta);
		}
	}

	@Override
	public void set(Metric<?> value) {
		for (MetricWriter writer : this.writers) {
			writer.set(value);
		}
	}

	@Override
	public void reset(String metricName) {
		for (MetricWriter writer : this.writers) {
			writer.reset(metricName);
		}
	}

}
