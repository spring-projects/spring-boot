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
import java.util.HashSet;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.MultiMetricRepository;
import org.springframework.boot.actuate.metrics.rich.RichGauge;
import org.springframework.boot.actuate.metrics.rich.RichGaugeReader;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;

/**
 * Exporter or converter for {@link RichGauge} data to a metric-based back end. Each gauge
 * measurement is stored as a set of related metrics with a common prefix (the name of the
 * gauge), and suffixes that describe the data. For example, a gauge called
 * <code>foo</code> is stored as
 * <code>[foo.min, foo.max. foo.val, foo.count, foo.avg, foo.alpha]</code>. If the
 * {@link MetricWriter} provided is a {@link MultiMetricRepository} then the values for a
 * gauge will be stored as a group, and hence will be retrievable from the repository in a
 * single query (or optionally individually).
 * 
 * @author Dave Syer
 */
public class RichGaugeExporter extends AbstractMetricExporter {

	private static final String MIN = ".min";

	private static final String MAX = ".max";

	private static final String COUNT = ".count";

	private static final String VALUE = ".val";

	private static final String AVG = ".avg";

	private static final String ALPHA = ".alpha";

	private final RichGaugeReader reader;
	private final MetricWriter writer;

	public RichGaugeExporter(RichGaugeReader reader, MetricWriter writer) {
		this(reader, writer, "");
	}

	public RichGaugeExporter(RichGaugeReader reader, MetricWriter writer, String prefix) {
		super(prefix);
		this.reader = reader;
		this.writer = writer;
	}

	@Override
	protected Iterable<Metric<?>> next(String group) {
		RichGauge rich = this.reader.findOne(group);
		Collection<Metric<?>> metrics = new ArrayList<Metric<?>>();
		metrics.add(new Metric<Number>(group + MIN, rich.getMin()));
		metrics.add(new Metric<Number>(group + MAX, rich.getMax()));
		metrics.add(new Metric<Number>(group + COUNT, rich.getCount()));
		metrics.add(new Metric<Number>(group + VALUE, rich.getValue()));
		metrics.add(new Metric<Number>(group + AVG, rich.getAverage()));
		metrics.add(new Metric<Number>(group + ALPHA, rich.getAlpha()));
		return metrics;
	}

	@Override
	protected Iterable<String> groups() {
		Collection<String> names = new HashSet<String>();
		for (RichGauge rich : this.reader.findAll()) {
			names.add(rich.getName());
		}
		return names;
	}

	@Override
	protected void write(String group, Collection<Metric<?>> values) {
		if (this.writer instanceof MultiMetricRepository) {
			((MultiMetricRepository) this.writer).save(group, values);
		}
		else {
			for (Metric<?> value : values) {
				this.writer.set(value);
			}
		}
	}

}
