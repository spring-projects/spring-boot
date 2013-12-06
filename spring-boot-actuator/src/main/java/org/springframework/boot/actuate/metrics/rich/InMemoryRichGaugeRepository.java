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

package org.springframework.boot.actuate.metrics.rich;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.util.SimpleInMemoryRepository;
import org.springframework.boot.actuate.metrics.util.SimpleInMemoryRepository.Callback;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;

/**
 * In memory implementation of {@link MetricWriter} and {@link RichGaugeReader}. When you
 * set a metric value (using {@link MetricWriter#set(Metric)}) it is used to update a rich
 * gauge (increment is a no-op). Gauge values can then be read out using the reader
 * operations.
 * 
 * @author Dave Syer
 */
public class InMemoryRichGaugeRepository implements RichGaugeRepository {

	private SimpleInMemoryRepository<RichGauge> repository = new SimpleInMemoryRepository<RichGauge>();

	@Override
	public void increment(Delta<?> delta) {
		// No-op
	}

	@Override
	public void set(Metric<?> metric) {

		final String name = metric.getName();
		final double value = metric.getValue().doubleValue();
		this.repository.update(name, new Callback<RichGauge>() {
			@Override
			public RichGauge modify(RichGauge current) {
				if (current == null) {
					current = new RichGauge(name, value);
				}
				else {
					current.set(value);
				}
				return current;
			}
		});

	}

	@Override
	public void reset(String metricName) {
		this.repository.remove(metricName);
	}

	@Override
	public RichGauge findOne(String metricName) {
		return this.repository.findOne(metricName);
	}

	@Override
	public Iterable<RichGauge> findAll() {
		return this.repository.findAll();
	}

	@Override
	public long count() {
		return this.repository.count();
	}
}
