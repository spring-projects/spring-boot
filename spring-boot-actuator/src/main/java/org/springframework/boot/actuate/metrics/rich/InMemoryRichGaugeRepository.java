/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.metrics.rich;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.util.SimpleInMemoryRepository;
import org.springframework.boot.actuate.metrics.util.SimpleInMemoryRepository.Callback;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;

/**
 * In memory implementation of {@link MetricWriter} and {@link RichGaugeReader}. When you
 * {@link MetricWriter#set(Metric) set} or {@link MetricWriter#increment(Delta) increment}
 * a metric value it is used to update a {@link RichGauge}. Gauge values can then be read
 * out using the reader operations.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class InMemoryRichGaugeRepository implements RichGaugeRepository {

	private final SimpleInMemoryRepository<RichGauge> repository = new SimpleInMemoryRepository<RichGauge>();

	@Override
	public void increment(final Delta<?> delta) {
		this.repository.update(delta.getName(), new Callback<RichGauge>() {

			@Override
			public RichGauge modify(RichGauge current) {
				double value = ((Number) delta.getValue()).doubleValue();
				if (current == null) {
					return new RichGauge(delta.getName(), value);
				}
				current.set(current.getValue() + value);
				return current;
			}

		});
	}

	@Override
	public void set(Metric<?> metric) {
		final String name = metric.getName();
		final double value = metric.getValue().doubleValue();
		this.repository.update(name, new Callback<RichGauge>() {

			@Override
			public RichGauge modify(RichGauge current) {
				if (current == null) {
					return new RichGauge(name, value);
				}
				current.set(value);
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
