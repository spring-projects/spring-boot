/*
 * Copyright 2014-2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.MultiMetricRepository;

/**
 * A {@link RichGaugeReader} that works by reading metric values from a
 * {@link MultiMetricRepository} where the group name is the RichGauge name. The format
 * used matches that in he RichGaugeExporter, so this reader can be used on a store that
 * has been populated using that exporter.
 * 
 * @author Dave Syer
 * @since 1.1.0
 */
public class MultiMetricRichGaugeReader implements RichGaugeReader {

	private static final String COUNT = ".count";
	private static final String MAX = ".max";
	private static final String MIN = ".min";
	private static final String AVG = ".avg";
	private static final String ALPHA = ".alpha";
	private static final String VAL = ".val";

	private final MultiMetricRepository repository;

	public MultiMetricRichGaugeReader(MultiMetricRepository repository) {
		this.repository = repository;
	}

	@Override
	public RichGauge findOne(String name) {
		Iterable<Metric<?>> metrics = this.repository.findAll(name);
		double value = 0;
		double average = 0.;
		double alpha = -1.;
		double min = 0.;
		double max = 0.;
		long count = 0;
		for (Metric<?> metric : metrics) {
			if (metric.getName().endsWith(VAL)) {
				value = metric.getValue().doubleValue();
			}
			else if (metric.getName().endsWith(ALPHA)) {
				alpha = metric.getValue().doubleValue();
			}
			else if (metric.getName().endsWith(AVG)) {
				average = metric.getValue().doubleValue();
			}
			else if (metric.getName().endsWith(MIN)) {
				min = metric.getValue().doubleValue();
			}
			else if (metric.getName().endsWith(MAX)) {
				max = metric.getValue().doubleValue();
			}
			else if (metric.getName().endsWith(COUNT)) {
				count = metric.getValue().longValue();
			}
		}
		return new RichGauge(name, value, alpha, average, max, min, count);
	}

	@Override
	public Iterable<RichGauge> findAll() {
		List<RichGauge> result = new ArrayList<RichGauge>();
		for (String name : this.repository.groups()) {
			result.add(findOne(name));
		}
		return result;
	}

	@Override
	public long count() {
		return this.repository.countGroups();
	}

}
