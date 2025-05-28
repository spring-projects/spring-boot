/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.metrics.autoconfigure.export.otlp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import io.micrometer.registry.otlp.AggregationTemporality;
import io.micrometer.registry.otlp.HistogramFlavor;
import io.micrometer.registry.otlp.OtlpConfig;

import org.springframework.boot.metrics.autoconfigure.export.otlp.OtlpMetricsProperties.Meter;
import org.springframework.boot.metrics.autoconfigure.export.properties.StepRegistryPropertiesConfigAdapter;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetryProperties;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetryResourceAttributes;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;

/**
 * Adapter to convert {@link OtlpMetricsProperties} to an {@link OtlpConfig}.
 *
 * @author Eddú Meléndez
 * @author Jonatan Ivanov
 * @author Moritz Halbritter
 */
class OtlpMetricsPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<OtlpMetricsProperties>
		implements OtlpConfig {

	private final OpenTelemetryProperties openTelemetryProperties;

	private final OtlpMetricsConnectionDetails connectionDetails;

	private final Environment environment;

	OtlpMetricsPropertiesConfigAdapter(OtlpMetricsProperties properties,
			OpenTelemetryProperties openTelemetryProperties, OtlpMetricsConnectionDetails connectionDetails,
			Environment environment) {
		super(properties);
		this.connectionDetails = connectionDetails;
		this.openTelemetryProperties = openTelemetryProperties;
		this.environment = environment;
	}

	@Override
	public String prefix() {
		return "management.otlp.metrics.export";
	}

	@Override
	public String url() {
		return get((properties) -> this.connectionDetails.getUrl(), OtlpConfig.super::url);
	}

	@Override
	public AggregationTemporality aggregationTemporality() {
		return get(OtlpMetricsProperties::getAggregationTemporality, OtlpConfig.super::aggregationTemporality);
	}

	@Override
	public Map<String, String> resourceAttributes() {
		Map<String, String> resourceAttributes = new LinkedHashMap<>();
		new OpenTelemetryResourceAttributes(this.environment, this.openTelemetryProperties.getResourceAttributes())
			.applyTo(resourceAttributes::put);
		return Collections.unmodifiableMap(resourceAttributes);
	}

	@Override
	public Map<String, String> headers() {
		return get(OtlpMetricsProperties::getHeaders, OtlpConfig.super::headers);
	}

	@Override
	public HistogramFlavor histogramFlavor() {
		return get(OtlpMetricsProperties::getHistogramFlavor, OtlpConfig.super::histogramFlavor);
	}

	@Override
	public Map<String, HistogramFlavor> histogramFlavorPerMeter() {
		return get(perMeter(Meter::getHistogramFlavor), OtlpConfig.super::histogramFlavorPerMeter);
	}

	@Override
	public Map<String, Integer> maxBucketsPerMeter() {
		return get(perMeter(Meter::getMaxBucketCount), OtlpConfig.super::maxBucketsPerMeter);
	}

	@Override
	public int maxScale() {
		return get(OtlpMetricsProperties::getMaxScale, OtlpConfig.super::maxScale);
	}

	@Override
	public int maxBucketCount() {
		return get(OtlpMetricsProperties::getMaxBucketCount, OtlpConfig.super::maxBucketCount);
	}

	@Override
	public TimeUnit baseTimeUnit() {
		return get(OtlpMetricsProperties::getBaseTimeUnit, OtlpConfig.super::baseTimeUnit);
	}

	private <V> Function<OtlpMetricsProperties, Map<String, V>> perMeter(Function<Meter, V> getter) {
		return (properties) -> {
			if (CollectionUtils.isEmpty(properties.getMeter())) {
				return null;
			}
			Map<String, V> perMeter = new LinkedHashMap<>();
			properties.getMeter().forEach((key, meterProperties) -> {
				V value = getter.apply(meterProperties);
				if (value != null) {
					perMeter.put(key, value);
				}
			});
			return (!perMeter.isEmpty()) ? perMeter : null;
		};

	}

}
