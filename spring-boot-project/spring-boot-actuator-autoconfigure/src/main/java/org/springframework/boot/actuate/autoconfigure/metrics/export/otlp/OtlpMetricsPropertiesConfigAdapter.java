/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.otlp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.micrometer.registry.otlp.AggregationTemporality;
import io.micrometer.registry.otlp.HistogramFlavor;
import io.micrometer.registry.otlp.OtlpConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;
import org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Adapter to convert {@link OtlpMetricsProperties} to an {@link OtlpConfig}.
 *
 * @author Eddú Meléndez
 * @author Jonatan Ivanov
 * @author Moritz Halbritter
 */
class OtlpMetricsPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<OtlpMetricsProperties>
		implements OtlpConfig {

	/**
	 * Default value for application name if {@code spring.application.name} is not set.
	 */
	private static final String DEFAULT_APPLICATION_NAME = "unknown_service";

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
	@SuppressWarnings("removal")
	public Map<String, String> resourceAttributes() {
		Map<String, String> resourceAttributes = this.openTelemetryProperties.getResourceAttributes();
		Map<String, String> result = new HashMap<>((!CollectionUtils.isEmpty(resourceAttributes)) ? resourceAttributes
				: get(OtlpMetricsProperties::getResourceAttributes, OtlpConfig.super::resourceAttributes));
		result.computeIfAbsent("service.name", (key) -> getApplicationName());
		result.computeIfAbsent("service.group", (key) -> getApplicationGroup());
		return Collections.unmodifiableMap(result);
	}

	private String getApplicationName() {
		return this.environment.getProperty("spring.application.name", DEFAULT_APPLICATION_NAME);
	}

	private String getApplicationGroup() {
		String applicationGroup = this.environment.getProperty("spring.application.group");
		return (StringUtils.hasLength(applicationGroup)) ? applicationGroup : null;
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

}
