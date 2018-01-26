/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.time.Duration;
import java.util.function.Function;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.instrument.histogram.HistogramConfig;
import io.micrometer.core.lang.Nullable;

/**
 * Allow for property-based {@link MeterFilter} configuration.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
public class PropertiesMeterFilter implements MeterFilter {
	private final MetricsProperties props;

	public PropertiesMeterFilter(MetricsProperties props) {
		this.props = props;
	}

	@Override
	public MeterFilterReply accept(Meter.Id id) {
		Boolean enabled = getMostSpecific(name -> this.props.getEnabled().get(name), id.getName());
		if (enabled != null && !enabled) {
			return MeterFilterReply.DENY;
		}
		return MeterFilterReply.NEUTRAL;
	}

	@Nullable
	private <V> V getMostSpecific(Function<String, V> lookup, String k) {
		V v = lookup.apply(k.isEmpty() ? "" : k);
		if (v != null) {
			return v;
		}
		else if (k.isEmpty() || k.equals("all")) {
			return null;
		}

		int lastSep = k.lastIndexOf('.');
		if (lastSep == -1) {
			return getMostSpecific(lookup, "all");
		}

		return getMostSpecific(lookup, k.substring(0, lastSep));
	}

	@Override
	public HistogramConfig configure(Meter.Id id, HistogramConfig config) {
		if (!id.getType().equals(Meter.Type.Timer) && !id.getType().equals(Meter.Type.DistributionSummary)) {
			return config;
		}

		HistogramConfig.Builder builder = HistogramConfig.builder();

		Boolean percentileHistogram = getMostSpecific(
				name -> this.props.getSummaries().getPercentileHistogram().getOrDefault(name, this.props.getTimers().getPercentileHistogram().get(name)),
				id.getName());
		if (percentileHistogram != null) {
			builder.percentilesHistogram(percentileHistogram);
		}

		double[] percentiles = getMostSpecific(
				name -> this.props.getSummaries().getPercentiles().getOrDefault(name, this.props.getTimers().getPercentiles().get(name)),
				id.getName());

		if (percentiles != null) {
			builder.percentiles(percentiles);
		}

		Integer histogramBufferLength = getMostSpecific(
				name -> this.props.getSummaries().getHistogramBufferLength().getOrDefault(name, this.props.getTimers().getHistogramBufferLength().get(name)),
				id.getName());
		if (histogramBufferLength != null) {
			builder.histogramBufferLength(histogramBufferLength);
		}

		Duration histogramExpiry = getMostSpecific(
				name -> this.props.getSummaries().getHistogramExpiry().getOrDefault(name, this.props.getTimers().getHistogramExpiry().get(name)),
				id.getName());
		if (histogramExpiry != null) {
			builder.histogramExpiry(histogramExpiry);
		}

		if (id.getType().equals(Meter.Type.Timer)) {
			Duration max = getMostSpecific(name -> this.props.getTimers().getMaximumExpectedValue().get(name), id.getName());
			if (max != null) {
				builder.maximumExpectedValue(max.toNanos());
			}

			Duration min = getMostSpecific(name -> this.props.getTimers().getMinimumExpectedValue().get(name), id.getName());
			if (min != null) {
				builder.minimumExpectedValue(min.toNanos());
			}

			Duration[] sla = getMostSpecific(name -> this.props.getTimers().getSla().get(name), id.getName());
			if (sla != null) {
				long[] slaNanos = new long[sla.length];
				for (int i = 0; i < sla.length; i++) {
					slaNanos[i] = sla[i].toNanos();
				}
				builder.sla(slaNanos);
			}
		}
		else if (id.getType().equals(Meter.Type.DistributionSummary)) {
			Long max = getMostSpecific(name -> this.props.getSummaries().getMaximumExpectedValue().get(name), id.getName());
			if (max != null) {
				builder.maximumExpectedValue(max);
			}

			Long min = getMostSpecific(name -> this.props.getSummaries().getMinimumExpectedValue().get(name), id.getName());
			if (min != null) {
				builder.minimumExpectedValue(min);
			}

			long[] sla = getMostSpecific(name -> this.props.getSummaries().getSla().get(name), id.getName());
			if (sla != null) {
				builder.sla(sla);
			}
		}

		return builder.build().merge(config);
	}
}
