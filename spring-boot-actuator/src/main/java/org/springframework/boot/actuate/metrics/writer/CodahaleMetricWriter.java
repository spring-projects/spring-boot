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

package org.springframework.boot.actuate.metrics.writer;

import org.springframework.boot.actuate.metrics.Metric;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * A {@link MetricWriter} that send data to a Codahale {@link MetricRegistry} based on a
 * naming convention:
 *
 * <ul>
 * <li>Updates to {@link #increment(Delta)} with names in "meter.*" are treated as
 * {@link Meter} events</li>
 * <li>Other deltas are treated as simple {@link Counter} values</li>
 * <li>Inputs to {@link #set(Metric)} with names in "histogram.*" are treated as
 * {@link Histogram} updates</li>
 * <li>Inputs to {@link #set(Metric)} with names in "timer.*" are treated as {@link Timer}
 * updates</li>
 * <li>Other metrics are treated as simple {@link Gauge} values (single valued
 * measurements of type double)</li>
 * </ul>
 *
 * @author Dave Syer
 * @deprecated since 1.2.2 in favor of {@link DropwizardMetricWriter}
 */
@Deprecated
public class CodahaleMetricWriter extends DropwizardMetricWriter {

	/**
	 * Create a new {@link DropwizardMetricWriter} instance.
	 * @param registry the underlying metric registry
	 */
	public CodahaleMetricWriter(MetricRegistry registry) {
		super(registry);
	}

}
