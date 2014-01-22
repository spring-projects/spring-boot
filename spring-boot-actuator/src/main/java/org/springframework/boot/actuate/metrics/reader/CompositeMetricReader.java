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

package org.springframework.boot.actuate.metrics.reader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.actuate.metrics.Metric;

/**
 * @author Dave Syer
 */
public class CompositeMetricReader implements MetricReader {

	private List<MetricReader> delegates = Collections.emptyList();

	public void setDelegates(Collection<MetricReader> delegates) {
		this.delegates = new ArrayList<MetricReader>(delegates);
	}

	@Override
	public Metric<?> findOne(String metricName) {
		for (MetricReader delegate : this.delegates) {
			Metric<?> value = delegate.findOne(metricName);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	@Override
	public Iterable<Metric<?>> findAll() {
		List<Metric<?>> values = new ArrayList<Metric<?>>((int) count());
		for (MetricReader delegate : this.delegates) {
			Iterable<Metric<?>> all = delegate.findAll();
			for (Metric<?> value : all) {
				values.add(value);
			}
		}
		return values;
	}

	@Override
	public long count() {
		long count = 0;
		for (MetricReader delegate : this.delegates) {
			count += delegate.count();

		}
		return count;
	}

}
