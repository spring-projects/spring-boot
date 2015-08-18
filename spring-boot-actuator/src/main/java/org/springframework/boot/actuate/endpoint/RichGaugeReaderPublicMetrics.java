/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.rich.RichGauge;
import org.springframework.boot.actuate.metrics.rich.RichGaugeReader;
import org.springframework.util.Assert;

/**
 * {@link PublicMetrics} exposed from a {@link RichGaugeReader}.
 *
 * @author Johannes Stelzer
 * @since 1.2
 */
public class RichGaugeReaderPublicMetrics implements PublicMetrics {

	private final RichGaugeReader richGaugeReader;

	public RichGaugeReaderPublicMetrics(RichGaugeReader richGaugeReader) {
		Assert.notNull(richGaugeReader, "RichGaugeReader must not be null");
		this.richGaugeReader = richGaugeReader;
	}

	@Override
	public Collection<Metric<?>> metrics() {
		List<Metric<?>> result = new ArrayList<Metric<?>>();
		for (RichGauge richGauge : this.richGaugeReader.findAll()) {
			result.addAll(convert(richGauge));
		}
		return result;
	}

	private List<Metric<?>> convert(RichGauge gauge) {
		List<Metric<?>> result = new ArrayList<Metric<?>>(6);
		result.add(new Metric<Double>(gauge.getName() + RichGauge.AVG, gauge.getAverage()));
		result.add(new Metric<Double>(gauge.getName() + RichGauge.VAL, gauge.getValue()));
		result.add(new Metric<Double>(gauge.getName() + RichGauge.MIN, gauge.getMin()));
		result.add(new Metric<Double>(gauge.getName() + RichGauge.MAX, gauge.getMax()));
		result.add(new Metric<Double>(gauge.getName() + RichGauge.ALPHA, gauge.getAlpha()));
		result.add(new Metric<Long>(gauge.getName() + RichGauge.COUNT, gauge.getCount()));
		return result;
	}

}
