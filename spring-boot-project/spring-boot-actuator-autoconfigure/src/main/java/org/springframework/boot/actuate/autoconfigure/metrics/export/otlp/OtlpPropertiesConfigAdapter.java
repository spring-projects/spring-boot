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
	private static final String DEFAULT_APPLICATION_NAME = "unknown_service";

	private final OpenTelemetryProperties openTelemetryProperties;

	private final OtlpMetricsConnectionDetails connectionDetails;

	private final Environment environment;

	/**
     * Constructs a new OtlpPropertiesConfigAdapter with the specified properties, openTelemetryProperties,
     * connectionDetails, and environment.
     * 
     * @param properties the OtlpProperties object containing the OTLP properties
     * @param openTelemetryProperties the OpenTelemetryProperties object containing the OpenTelemetry properties
     * @param connectionDetails the OtlpMetricsConnectionDetails object containing the OTLP metrics connection details
     * @param environment the Environment object containing the environment details
     */
    OtlpPropertiesConfigAdapter(OtlpProperties properties, OpenTelemetryProperties openTelemetryProperties,
			OtlpMetricsConnectionDetails connectionDetails, Environment environment) {
		super(properties);
		this.connectionDetails = connectionDetails;
		this.openTelemetryProperties = openTelemetryProperties;
		this.environment = environment;
	}

	/**
     * Returns the prefix for the OTLP metrics export configuration properties.
     *
     * @return the prefix for the OTLP metrics export configuration properties
     */
    @Override
	public String prefix() {
		return "management.otlp.metrics.export";
	}

	/**
     * Returns the URL for the connection.
     * 
     * @return the URL for the connection
     */
    @Override
	public String url() {
		return get((properties) -> this.connectionDetails.getUrl(), OtlpConfig.super::url);
	}

	/**
     * Returns the aggregation temporality for the OTLP properties.
     * 
     * @return the aggregation temporality
     */
    @Override
	public AggregationTemporality aggregationTemporality() {
		return get(OtlpProperties::getAggregationTemporality, OtlpConfig.super::aggregationTemporality);
	}

	/**
     * Returns the resource attributes for the OpenTelemetry properties.
     * 
     * @return a map containing the resource attributes
     * @deprecated This method is marked for removal and should not be used.
     */
    @Override
	@SuppressWarnings("removal")
	public Map<String, String> resourceAttributes() {
		Map<String, String> resourceAttributes = this.openTelemetryProperties.getResourceAttributes();
		Map<String, String> result = new HashMap<>((!CollectionUtils.isEmpty(resourceAttributes)) ? resourceAttributes
				: get(OtlpProperties::getResourceAttributes, OtlpConfig.super::resourceAttributes));
		result.computeIfAbsent("service.name", (key) -> getApplicationName());
		return Collections.unmodifiableMap(result);
	}

	/**
     * Returns the name of the application.
     * 
     * @return the name of the application
     */
    private String getApplicationName() {
		return this.environment.getProperty("spring.application.name", DEFAULT_APPLICATION_NAME);
	}

	/**
     * Returns the headers for the OTLP properties.
     *
     * @return a map containing the headers
     */
    @Override
	public Map<String, String> headers() {
		return get(OtlpProperties::getHeaders, OtlpConfig.super::headers);
	}

	/**
     * Returns the base time unit for the OTLP properties.
     * 
     * @return the base time unit
     */
    @Override
	public TimeUnit baseTimeUnit() {
		return get(OtlpProperties::getBaseTimeUnit, OtlpConfig.super::baseTimeUnit);
	}

}
