/*
 * Copyright 2012-2023 the original author or authors.
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
import io.micrometer.registry.otlp.OtlpConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;
import org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;

/**
 * Adapter to convert {@link OtlpProperties} to an {@link OtlpConfig}.
 *
 * @author Eddú Meléndez
 * @author Jonatan Ivanov
 * @author Moritz Halbritter
 */
class OtlpPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<OtlpProperties> implements OtlpConfig {

	/**
	 * Default value for application name if {@code spring.application.name} is not set.
	 */
	private static final String DEFAULT_APPLICATION_NAME = "application";

	private final OpenTelemetryProperties openTelemetryProperties;

	private final OtlpMetricsConnectionDetails connectionDetails;

	private final Environment environment;

	OtlpPropertiesConfigAdapter(OtlpProperties properties, OpenTelemetryProperties openTelemetryProperties,
			OtlpMetricsConnectionDetails connectionDetails, Environment environment) {
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
		return get(OtlpProperties::getAggregationTemporality, OtlpConfig.super::aggregationTemporality);
	}

	@Override
	@SuppressWarnings("removal")
	public Map<String, String> resourceAttributes() {
		Map<String, String> result;
		if (!CollectionUtils.isEmpty(this.openTelemetryProperties.getResourceAttributes())) {
			result = new HashMap<>(this.openTelemetryProperties.getResourceAttributes());
		}
		else {
			result = new HashMap<>(get(OtlpProperties::getResourceAttributes, OtlpConfig.super::resourceAttributes));
		}
		result.computeIfAbsent("service.name",
				(ignore) -> this.environment.getProperty("spring.application.name", DEFAULT_APPLICATION_NAME));
		return Collections.unmodifiableMap(result);
	}

	@Override
	public Map<String, String> headers() {
		return get(OtlpProperties::getHeaders, OtlpConfig.super::headers);
	}

	@Override
	public TimeUnit baseTimeUnit() {
		return get(OtlpProperties::getBaseTimeUnit, OtlpConfig.super::baseTimeUnit);
	}

}
